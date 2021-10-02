package dragons.vulkan.memory

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkImageMemoryBarrier

fun transitionImageLayout(
    commandBuffer: VkCommandBuffer, image: Long, stack: MemoryStack,
    oldLayout: Int, newLayout: Int, srcStage: Int, dstStage: Int, srcAccessMask: Int, dstAccessMask: Int, aspectMask: Int
) {
    val barriers = VkImageMemoryBarrier.calloc(1, stack)
    val barrier = barriers[0]
    barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
    barrier.oldLayout(oldLayout)
    barrier.newLayout(newLayout)
    barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
    barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
    barrier.srcAccessMask(srcAccessMask)
    barrier.dstAccessMask(dstAccessMask)
    barrier.image(image)
    barrier.subresourceRange { ssr ->
        ssr.aspectMask(aspectMask)
        // TODO Add support for multiple mip levels and array layers
        ssr.baseMipLevel(0)
        ssr.levelCount(1)
        ssr.baseArrayLayer(0)
        ssr.layerCount(1)
    }

    vkCmdPipelineBarrier(
        commandBuffer, srcStage, dstStage, 0, null, null, barriers
    )
}