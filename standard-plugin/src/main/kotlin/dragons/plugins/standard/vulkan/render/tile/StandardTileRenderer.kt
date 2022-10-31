package dragons.plugins.standard.vulkan.render.tile

import dragons.plugins.standard.vulkan.pipeline.BasicGraphicsPipeline
import dragons.plugins.standard.vulkan.render.TransformationMatrixManager
import dragons.plugins.standard.vulkan.render.chunk.ChunkTilesRenderEntry
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.vulkan.memory.VulkanBuffer
import dragons.vulkan.memory.VulkanBufferRange
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDrawIndirectCount.vkCmdDrawIndexedIndirectCountKHR
import org.lwjgl.vulkan.VK10.*
import java.nio.IntBuffer

internal class StandardTileRenderer(
    private val indirectDrawIntBuffer: IntBuffer,
    private val indirectDrawVulkanBuffer: VulkanBufferRange,
    private val transformationMatrixManager: TransformationMatrixManager
) {

    private val chunkEntries = mutableMapOf<VulkanBuffer, ChunkTilesRenderEntry>()

    internal var shouldRecordCommandsAgain = false
        private set
    private var isAcceptingDrawCommands = false

    init {
        if (this.indirectDrawIntBuffer.limit().toLong() * Int.SIZE_BYTES != this.indirectDrawVulkanBuffer.size) {
            throw IllegalArgumentException("The size of the indirect draw buffers must be equal")
        }
    }

    fun recordCommandsDuringRenderPass(
        commandBuffer: VkCommandBuffer, pipeline: BasicGraphicsPipeline, staticDescriptorSet: Long
    ) {
        if (this.isAcceptingDrawCommands) throw IllegalStateException("Can't record commands between startFrame() and endFrame()")

        stackPush().use { stack ->
            for (chunkEntry in this.chunkEntries.values) {
                vkCmdBindDescriptorSets(
                    commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.pipelineLayout,
                    0, stack.longs(staticDescriptorSet, chunkEntry.dynamicDescriptorSet), null
                )
                vkCmdBindVertexBuffers(
                    commandBuffer, 0,
                    stack.longs(chunkEntry.vertexBuffer.handle),
                    stack.longs(0)
                )
                vkCmdBindIndexBuffer(
                    commandBuffer,
                    chunkEntry.indexBuffer.handle,
                    0,
                    VK_INDEX_TYPE_UINT32
                )
                vkCmdDrawIndexedIndirectCountKHR(
                    commandBuffer,
                    this.indirectDrawVulkanBuffer.buffer.handle,
                    this.indirectDrawVulkanBuffer.offset + chunkEntry.indirectDrawIndex.toLong() * Int.SIZE_BYTES,
                    this.indirectDrawVulkanBuffer.buffer.handle,
                    this.indirectDrawVulkanBuffer.offset + chunkEntry.indirectCountIndex.toLong() * Int.SIZE_BYTES,
                    chunkEntry.maxNumIndirectDrawCalls,
                    VkDrawIndexedIndirectCommand.SIZEOF
                )
            }
        }

        this.shouldRecordCommandsAgain = false
    }

    fun startFrame() {
        if (this.isAcceptingDrawCommands) throw IllegalStateException("Already started this frame")

        for (chunkEntry in this.chunkEntries.values) {
            chunkEntry.currentDrawCount = 0
        }

        this.isAcceptingDrawCommands = true
    }

    fun endFrame() {
        if (!this.isAcceptingDrawCommands) throw IllegalStateException("Didn't start this frame yet")
        this.isAcceptingDrawCommands = false
        if (this.shouldRecordCommandsAgain) throw IllegalStateException("Call recordCommands() first")

        for (chunkEntry in this.chunkEntries.values) {
            this.indirectDrawIntBuffer.put(chunkEntry.indirectCountIndex, chunkEntry.currentDrawCount)
        }
    }

    fun addChunk(
        vertexBuffer: VulkanBuffer,
        indexBuffer: VulkanBuffer,
        dynamicDescriptorSet: Long,
        maxNumIndirectDrawCalls: Int
    ) {
        if (this.chunkEntries.containsKey(vertexBuffer)) {
            throw IllegalStateException("This chunk has been added already")
        }
        if (this.isAcceptingDrawCommands) {
            throw IllegalStateException("Can't add new chunks between startFrame() and endFrame()")
        }

        val indirectDrawIndex = this.chunkEntries.maxOfOrNull { it.value.indirectCountIndex + 1 } ?: 0
        val indirectCountIndex = indirectDrawIndex + (VkDrawIndexedIndirectCommand.SIZEOF / Int.SIZE_BYTES) * maxNumIndirectDrawCalls

        if (indirectCountIndex >= this.indirectDrawIntBuffer.limit()) {
            throw IndexOutOfBoundsException(
                "Limit of indirect draw buffer is ${this.indirectDrawIntBuffer.limit()}, but $indirectCountIndex ints are needed"
            )
        }

        this.chunkEntries[vertexBuffer] = ChunkTilesRenderEntry(
            dynamicDescriptorSet = dynamicDescriptorSet,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            indirectDrawIndex = indirectDrawIndex,
            indirectCountIndex = indirectCountIndex,
            maxNumIndirectDrawCalls = maxNumIndirectDrawCalls
        )
        this.shouldRecordCommandsAgain = true
    }

    fun drawTile(
        vertices: VulkanBufferRange,
        indices: VulkanBufferRange,
        transformationMatrices: Array<Matrix4f>
    ) {
        if (!this.isAcceptingDrawCommands) throw IllegalStateException("Drawing is only possible between startFrame() and endFrame()")

        if (indices.size % 4L != 0L) throw IllegalArgumentException("Size of indices must be a multiple of 4")
        if (indices.offset % 4L != 0L) throw IllegalArgumentException("Offset of indices must be a multiple of 4")
        if (vertices.offset % BasicVertex.SIZE != 0L) throw IllegalArgumentException("Offset of vertices must be a multiple of BasicVertex.SIZE")

        val chunkEntry = this.chunkEntries[vertices.buffer] ?: throw IllegalArgumentException("No loaded chunk has this vertex buffer")
        if (indices.buffer != chunkEntry.indexBuffer) {
            throw IllegalArgumentException("The chunk with the given vertex buffer has a different index buffer")
        }

        val firstMatrixIndex = this.transformationMatrixManager.prepareMatrices(transformationMatrices)

        // TODO Handle currentDrawCount atomically
        val drawCommand = VkDrawIndexedIndirectCommand.create(memAddress(
            this.indirectDrawIntBuffer,
            chunkEntry.indirectDrawIndex + chunkEntry.currentDrawCount * (VkDrawIndexedIndirectCommand.SIZEOF / Int.SIZE_BYTES)
        ))
        drawCommand.indexCount((indices.size / 4).toInt())
        drawCommand.instanceCount(transformationMatrices.size)
        drawCommand.firstIndex((indices.offset / 4).toInt())
        drawCommand.vertexOffset((vertices.offset / BasicVertex.SIZE).toInt())
        drawCommand.firstInstance(firstMatrixIndex)

        chunkEntry.currentDrawCount += 1
    }

    // TODO Make it possible to remove chunks
}
