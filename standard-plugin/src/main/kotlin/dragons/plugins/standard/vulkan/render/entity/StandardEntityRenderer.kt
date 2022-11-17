package dragons.plugins.standard.vulkan.render.entity

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.plugins.standard.vulkan.pipeline.createBasicDynamicDescriptorPool
import dragons.plugins.standard.vulkan.pipeline.updateBasicDynamicDescriptorSet
import dragons.plugins.standard.vulkan.render.TransformationMatrixManager
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.state.StaticGraphicsState
import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.util.assertVkSuccess
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDrawIndirectCount.vkCmdDrawIndexedIndirectCountKHR
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer
import java.nio.IntBuffer

internal class StandardEntityRenderer(
    private val graphicsState: StaticGraphicsState,
    basicDynamicDescriptorSetLayout: Long,
    private val meshStagingBuffer: VulkanBufferRange,
    private val meshHostBuffer: ByteBuffer,
    private val meshDeviceBuffer: VulkanBufferRange,
    private val indirectDrawIntBuffer: IntBuffer,
    private val indirectDrawVulkanBuffer: VulkanBufferRange,
    private val transformationMatrixManager: TransformationMatrixManager,
    maxNumPixels: Long
) {

    private val currentMeshes = mutableListOf<MeshToDraw>()
    private val meshTracker = EntityMeshTracker(this.meshDeviceBuffer.size.toInt())
    private val imageTracker = EntityImageTracker(graphicsState, maxNumPixels)

    private val dynamicDescriptorPool: Long
    private val dynamicDescriptorSet: Long

    // Note: it is initially false because it needs to be set to true during endFrame()
    internal var shouldRecordCommandsAgain = false
        private set
    private var didRenderAnything = false
    private var isAcceptingDrawCommands = false

    init {
        if (this.meshStagingBuffer.size > Int.MAX_VALUE.toLong()) {
            throw IllegalArgumentException("Mesh staging buffer is too big")
        }
        if (this.meshHostBuffer.capacity() != this.meshStagingBuffer.size.toInt()) {
            throw IllegalArgumentException(
                "The size of the host buffer (${this.meshHostBuffer.capacity()}) must be equal to the size " +
                        "of the staging buffer (${this.meshStagingBuffer.size})"
            )
        }
        if (this.meshStagingBuffer.size != this.meshDeviceBuffer.size) {
            throw IllegalArgumentException(
                "The size of the staging buffer (${this.meshStagingBuffer.size}) must be equal to the size of the " +
                        "device buffer (${this.meshDeviceBuffer.size})"
            )
        }

        this.dynamicDescriptorPool = createBasicDynamicDescriptorPool(graphicsState.vkDevice, 1)

        stackPush().use { stack ->
            val aiDescriptorSet = VkDescriptorSetAllocateInfo.calloc(stack)
            aiDescriptorSet.`sType$Default`()
            aiDescriptorSet.descriptorPool(this.dynamicDescriptorPool)
            aiDescriptorSet.pSetLayouts(stack.longs(basicDynamicDescriptorSetLayout))

            val pDescriptorSet = stack.callocLong(1)
            assertVkSuccess(
                vkAllocateDescriptorSets(graphicsState.vkDevice, aiDescriptorSet, pDescriptorSet),
                "AllocateDescriptorSets", "dynamic entity renderer"
            )
            this.dynamicDescriptorSet = pDescriptorSet[0]
        }
    }

    fun startFrame() {
        if (this.isAcceptingDrawCommands) throw IllegalStateException("Already started this frame")
        this.isAcceptingDrawCommands = true
        this.didRenderAnything = false

        this.currentMeshes.clear()
        this.meshTracker.startFrame()
        this.imageTracker.startFrame()
    }

    fun drawMesh(mesh: EntityMesh, transformationMatrices: Array<Matrix4f>) {
        if (!this.isAcceptingDrawCommands) throw IllegalStateException("Drawing is only possible between startFrame() and endFrame()")

        val firstMatrixIndex = this.transformationMatrixManager.prepareMatrices(transformationMatrices)

        // Unlike the transformation matrix manager, the mesh/image tracker and the list of current meshes are not thread-safe
        synchronized(this) {
            this.meshTracker.useMesh(mesh)
            this.imageTracker.useMesh(mesh)
            this.currentMeshes.add(MeshToDraw(mesh, firstMatrixIndex))
        }
        this.didRenderAnything = true
    }

    fun recordCommandsBeforeRenderPass(
        commandBuffer: VkCommandBuffer
    ) {
        if (!didRenderAnything) return

        val copyRanges = mutableListOf<Pair<Int, Int>>()

        for (meshToDraw in this.currentMeshes) {
            val mesh = meshToDraw.mesh
            val location = this.meshTracker.getLocation(mesh)
            if (!location.hasBeenFilled) {

                val vertexBuffer = BasicVertex.createList(this.meshHostBuffer, location.vertexOffset, mesh.generator.numVertices.toLong())
                val indexBuffer = memSlice(this.meshHostBuffer, location.indexOffset, Int.SIZE_BYTES * mesh.generator.numIndices).asIntBuffer()
                val (colorImageIndices, heightImageIndices) = this.imageTracker.getDescriptorIndices(mesh)

                // TODO Move this expensive function to the background
                mesh.generator.fillVertexBuffer(vertexBuffer, colorImageIndices, heightImageIndices)
                mesh.generator.fillIndexBuffer(indexBuffer)

                this.meshTracker.markFilled(mesh)
                copyRanges.add(Pair(location.vertexOffset, mesh.generator.numVertices * BasicVertex.SIZE))
                copyRanges.add(Pair(location.indexOffset, mesh.generator.numIndices * Int.SIZE_BYTES))
            }
        }

        stackPush().use { stack ->

            if (copyRanges.isNotEmpty()) {
                val pCopyRanges = VkBufferCopy.calloc(copyRanges.size, stack)
                for ((index, copyRange) in copyRanges.withIndex()) {
                    val (offset, size) = copyRange
                    pCopyRanges[index].srcOffset(this.meshStagingBuffer.offset + offset.toLong())
                    pCopyRanges[index].dstOffset(this.meshDeviceBuffer.offset + offset.toLong())
                    pCopyRanges[index].size(size.toLong())
                }

                val pBarriers = VkBufferMemoryBarrier.calloc(copyRanges.size, stack)
                for ((index, barrier) in pBarriers.withIndex()) {
                    barrier.`sType$Default`()
                    barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    barrier.size(copyRanges[index].second.toLong())
                }

                // Prepare staging buffer to send its content to the device buffer
                for ((index, barrier) in pBarriers.withIndex()) {
                    barrier.buffer(this.meshStagingBuffer.buffer.handle)
                    barrier.offset(this.meshStagingBuffer.offset + copyRanges[index].first)
                    barrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT)
                    barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                }
                vkCmdPipelineBarrier(
                    commandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                    null, pBarriers, null
                )

                // Prepare the device buffer to receive content from the staging buffer
                for ((index, barrier) in pBarriers.withIndex()) {
                    barrier.buffer(this.meshDeviceBuffer.buffer.handle)
                    barrier.offset(this.meshDeviceBuffer.offset + copyRanges[index].first)
                    barrier.srcAccessMask(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT or VK_ACCESS_INDEX_READ_BIT)
                    barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                }
                vkCmdPipelineBarrier(
                    commandBuffer, VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                    null, pBarriers, null
                )

                // Copy the staging buffer to the device buffer
                vkCmdCopyBuffer(
                    commandBuffer,
                    this.meshStagingBuffer.buffer.handle,
                    this.meshDeviceBuffer.buffer.handle,
                    pCopyRanges
                )

                // Prepare the staging buffer to be filled with new meshes (for subsequent frames)
                for ((index, barrier) in pBarriers.withIndex()) {
                    barrier.buffer(this.meshStagingBuffer.buffer.handle)
                    barrier.offset(this.meshStagingBuffer.offset + copyRanges[index].first)
                    barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                    barrier.dstAccessMask(VK_ACCESS_HOST_WRITE_BIT)
                }
                vkCmdPipelineBarrier(
                    commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_HOST_BIT, 0,
                    null, pBarriers, null
                )

                // Prepare the device buffer to present its vertices
                for ((index, barrier) in pBarriers.withIndex()) {
                    barrier.buffer(this.meshDeviceBuffer.buffer.handle)
                    barrier.offset(this.meshDeviceBuffer.offset + copyRanges[index].first)
                    barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    barrier.dstAccessMask(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT or VK_ACCESS_INDEX_READ_BIT)
                }
                vkCmdPipelineBarrier(
                    commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, 0,
                    null, pBarriers, null
                )

                updateBasicDynamicDescriptorSet(
                    this.graphicsState.vkDevice, this.dynamicDescriptorSet,
                    this.imageTracker.getCurrentlyUsedColorImages(),
                    this.imageTracker.getCurrentlyUsedHeightImages()
                )
            }
        }
    }

    fun recordCommandsDuringRenderPass(
        commandBuffer: VkCommandBuffer, pipeline: BasicGraphicsPipeline, staticDescriptorSet: Long
    ) {
        if (!didRenderAnything) return

        stackPush().use { stack ->
            vkCmdBindDescriptorSets(
                commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipeline.pipelineLayout,
                0, stack.longs(staticDescriptorSet, dynamicDescriptorSet), null
            )
            vkCmdBindVertexBuffers(
                commandBuffer, 0,
                stack.longs(this.meshDeviceBuffer.buffer.handle),
                stack.longs(this.meshDeviceBuffer.offset)
            )
            vkCmdBindIndexBuffer(
                commandBuffer,
                this.meshDeviceBuffer.buffer.handle,
                this.meshDeviceBuffer.offset,
                VK_INDEX_TYPE_UINT32
            )
            vkCmdDrawIndexedIndirectCountKHR(
                commandBuffer,
                this.indirectDrawVulkanBuffer.buffer.handle,
                this.indirectDrawVulkanBuffer.offset + Int.SIZE_BYTES,
                this.indirectDrawVulkanBuffer.buffer.handle,
                this.indirectDrawVulkanBuffer.offset,
                ((this.indirectDrawVulkanBuffer.size - 4) / BasicVertex.SIZE).toInt(), // TODO Pick a better maximum bound
                VkDrawIndexedIndirectCommand.SIZEOF
            )
        }

        this.shouldRecordCommandsAgain = false
    }

    fun endFrame() {
        if (!this.isAcceptingDrawCommands) throw IllegalStateException("Didn't start this frame yet")
        this.isAcceptingDrawCommands = false
        if (this.shouldRecordCommandsAgain) throw IllegalStateException("Call recordCommands() first")

        this.imageTracker.endFrame()
        this.meshTracker.endFrame()

        for ((index, meshToDraw) in this.currentMeshes.withIndex()) {
            val meshLocation = this.meshTracker.getLocation(meshToDraw.mesh)
            if (!meshLocation.hasBeenFilled) {
                this.shouldRecordCommandsAgain = true
            }

            val drawCommand = VkDrawIndexedIndirectCommand.create(memAddress(
                // Position 0 is reserved for the count
                this.indirectDrawIntBuffer, 1 + index * (VkDrawIndexedIndirectCommand.SIZEOF / Int.SIZE_BYTES)
            ))
            drawCommand.indexCount(meshToDraw.mesh.generator.numIndices)
            drawCommand.instanceCount(meshToDraw.mesh.numTransformationMatrices)
            drawCommand.firstIndex(meshLocation.indexOffset / Int.SIZE_BYTES)
            drawCommand.vertexOffset(meshLocation.vertexOffset / BasicVertex.SIZE)
            drawCommand.firstInstance(meshToDraw.firstMatrixIndex)
        }
        this.indirectDrawIntBuffer.put(0, this.currentMeshes.size)
    }

    fun destroy() {
        this.meshTracker.destroy()
        this.imageTracker.destroy()
        vkDestroyDescriptorPool(this.graphicsState.vkDevice, this.dynamicDescriptorPool, null)
    }
}

private class MeshToDraw(
    val mesh: EntityMesh,
    val firstMatrixIndex: Int
)
