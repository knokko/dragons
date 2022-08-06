package dragons.plugins.standard.vulkan.render

import dragons.vulkan.memory.VulkanBufferRange
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferMemoryBarrier
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.FloatBuffer

class TransformationMatrixManager(
    private val floatBuffer: FloatBuffer,
    private val hostBuffer: VulkanBufferRange,
    private val deviceBuffer: VulkanBufferRange
) {

    private var currentMatrixIndex = 0
    private var isWithinFrame = false

    init {
        if (this.hostBuffer.size != this.floatBuffer.limit().toLong() * Float.SIZE_BYTES || this.hostBuffer.size != this.deviceBuffer.size) {
            throw IllegalArgumentException("All buffer sizes must be equal")
        }
    }

    fun recordCommands(
        commandBuffer: VkCommandBuffer
    ) {
        stackPush().use { stack ->

            val preCopyBarriers = VkBufferMemoryBarrier.calloc(1, stack)
            val preCopyBarrier = preCopyBarriers[0]
            preCopyBarrier.`sType$Default`()
            preCopyBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT)
            preCopyBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
            preCopyBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            preCopyBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            preCopyBarrier.buffer(this.hostBuffer.buffer.handle)
            preCopyBarrier.offset(this.hostBuffer.offset)
            preCopyBarrier.size(this.hostBuffer.size)

            vkCmdPipelineBarrier(
                commandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                null, preCopyBarriers, null
            )

            // TODO Consider not copying (many) more matrices than actually used
            // (but note that this is not easy unless the commands are recorded every frame, which is not the case)
            val copyRegions = VkBufferCopy.calloc(1, stack)
            val copyRegion = copyRegions[0]
            copyRegion.srcOffset(this.hostBuffer.offset)
            copyRegion.dstOffset(this.deviceBuffer.offset)
            copyRegion.size(this.hostBuffer.size)

            vkCmdCopyBuffer(commandBuffer, this.hostBuffer.buffer.handle, this.deviceBuffer.buffer.handle, copyRegions)

            val postCopyBarriers = VkBufferMemoryBarrier.calloc(1, stack)
            val postCopyBarrier = postCopyBarriers[0]
            postCopyBarrier.`sType$Default`()
            postCopyBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            postCopyBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            postCopyBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            postCopyBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            postCopyBarrier.buffer(this.deviceBuffer.buffer.handle)
            postCopyBarrier.offset(this.deviceBuffer.offset)
            postCopyBarrier.size(this.deviceBuffer.size)

            vkCmdPipelineBarrier(
                commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                0, null, postCopyBarriers, null
            )
        }
    }

    fun startFrame() {
        if (this.isWithinFrame) throw IllegalStateException("Frame already started")

        this.currentMatrixIndex = 0
        this.isWithinFrame = true
    }

    fun endFrame() {
        if (!this.isWithinFrame) throw IllegalStateException("No frame is in progress")

        this.isWithinFrame = false
    }

    fun prepareMatrices(matrices: Array<Matrix4f>): Int {
        if (!this.isWithinFrame) throw IllegalStateException("No frame is in progress")

        // TODO Do this atomically
        val startMatrixIndex = this.currentMatrixIndex
        this.currentMatrixIndex += matrices.size

        for ((arrayMatrixIndex, matrix) in matrices.withIndex()) {
            val floatIndex = (startMatrixIndex + arrayMatrixIndex) * 16
            matrix.get(floatIndex, this.floatBuffer)
        }

        return startMatrixIndex
    }
}
