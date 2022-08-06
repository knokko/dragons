package dragons.plugins.standard.vulkan.render

import dragons.vulkan.memory.VulkanBufferRange
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferMemoryBarrier
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.FloatBuffer

class CameraBufferManager(
    private val floatBuffer: FloatBuffer,
    private val hostBuffer: VulkanBufferRange,
    private val deviceBuffer: VulkanBufferRange
) {

    init {
        if (this.floatBuffer.limit().toLong() * Float.SIZE_BYTES != this.hostBuffer.size || this.hostBuffer.size != this.deviceBuffer.size) {
            throw IllegalArgumentException("The size of all 3 camera buffers must be equal")
        }
        if (this.floatBuffer.limit() != 32) throw IllegalArgumentException("The size of the camera buffers should be 32 floats")
    }

    fun recordCommands(
        commandBuffer: VkCommandBuffer
    ) {
        // TODO Reduce code duplication with TransformationMatrixManager
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

            val copyRegions = VkBufferCopy.calloc(1, stack)
            val copyRegion = copyRegions[0]
            copyRegion.srcOffset(this.hostBuffer.offset)
            copyRegion.dstOffset(this.deviceBuffer.offset)
            copyRegion.size(this.hostBuffer.size)

            vkCmdCopyBuffer(
                commandBuffer,
                this.hostBuffer.buffer.handle,
                this.deviceBuffer.buffer.handle,
                copyRegions
            )

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

    fun setCamera(leftEyeMatrix: Matrix4f, rightEyeMatrix: Matrix4f) {
        leftEyeMatrix.get(0, this.floatBuffer)
        rightEyeMatrix.get(16, this.floatBuffer)
    }
}
