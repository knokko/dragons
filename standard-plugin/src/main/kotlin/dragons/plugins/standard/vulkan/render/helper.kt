package dragons.plugins.standard.vulkan.render

import dragons.vulkan.memory.VulkanBufferRange
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferMemoryBarrier
import org.lwjgl.vulkan.VkCommandBuffer

fun recordBufferCopyAndMemoryBarriers(
    commandBuffer: VkCommandBuffer, source: VulkanBufferRange, destination: VulkanBufferRange,
    srcAccessMask: Int, srcStageMask: Int, dstAccessMask: Int, dstStageMask: Int
) {
    if (source.size != destination.size) {
        throw IllegalArgumentException("Source size (${source.size}) must be equal to destination size (${destination.size})")
    }

    stackPush().use { stack ->
        val preCopyBarriers = VkBufferMemoryBarrier.calloc(1, stack)
        val preCopyBarrier = preCopyBarriers[0]
        preCopyBarrier.`sType$Default`()
        preCopyBarrier.srcAccessMask(srcAccessMask)
        preCopyBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
        preCopyBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        preCopyBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        preCopyBarrier.buffer(source.buffer.handle)
        preCopyBarrier.offset(source.offset)
        preCopyBarrier.size(source.size)

        vkCmdPipelineBarrier(
            commandBuffer, srcStageMask, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
            null, preCopyBarriers, null
        )

        val copyRegions = VkBufferCopy.calloc(1, stack)
        val copyRegion = copyRegions[0]
        copyRegion.srcOffset(source.offset)
        copyRegion.dstOffset(destination.offset)
        copyRegion.size(source.size)

        vkCmdCopyBuffer(
            commandBuffer,
            source.buffer.handle,
            destination.buffer.handle,
            copyRegions
        )

        val postCopyBarriers = VkBufferMemoryBarrier.calloc(1, stack)
        val postCopyBarrier = postCopyBarriers[0]
        postCopyBarrier.`sType$Default`()
        postCopyBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
        postCopyBarrier.dstAccessMask(dstAccessMask)
        postCopyBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        postCopyBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        postCopyBarrier.buffer(destination.buffer.handle)
        postCopyBarrier.offset(destination.offset)
        postCopyBarrier.size(destination.size)

        vkCmdPipelineBarrier(
            commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, dstStageMask,
            0, null, postCopyBarriers, null
        )
    }
}
