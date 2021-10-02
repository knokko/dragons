package dragons.vulkan.memory.scope

import dragons.vulkan.memory.VulkanImage
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDevice

/**
 * Represents a 'group of' resources that occupy GPU memory that will all live as long as the *scope*: the memory and
 * GPU resources are created/allocated when the *scope* is created and destroyed/freed when the *scope* is destroyed.
 *
 * The resources in a *scope* should be packed together in as little `VkMemory` objects as possible because graphics
 * cards typically allow only a very limited number of memory heaps. Also, the buffer resources should be packed in as
 * little `VkBuffer` objects as possible because this is often beneficial for performance (according to the best
 * practices overview of Nvidia). Packing these resources together is relatively easy because they are all created and
 * destroyed at the same time.
 *
 * The current plan is that there will be a global memory scope (for the resources that are needed in the entire game
 * or are extremely common), a memory scope for just the main menu, a memory scope for every realm, and a memory scope
 * for every chunk.
 */
class MemoryScope(
    val deviceBufferMemory: Long?,
    val deviceBuffers: Collection<Long>,
    val persistentStagingMemory: Long?,
    val persistentStagingBuffers: Collection<Long>,
    val deviceImageMemory: Long?,
    val deviceImages: Collection<VulkanImage>
) {

    fun destroy(vkDevice: VkDevice) {
        for (deviceBuffer in deviceBuffers) {
            vkDestroyBuffer(vkDevice, deviceBuffer, null)
        }
        if (deviceBufferMemory != null) {
            vkFreeMemory(vkDevice, deviceBufferMemory, null)
        }
        for (stagingBuffer in persistentStagingBuffers) {
            vkDestroyBuffer(vkDevice, stagingBuffer, null)
        }
        if (persistentStagingMemory != null) {
            vkFreeMemory(vkDevice, persistentStagingMemory, null)
        }
        for (deviceImage in deviceImages) {
            vkDestroyImageView(vkDevice, deviceImage.fullView, null)
            vkDestroyImage(vkDevice, deviceImage.handle, null)
        }
        if (deviceImageMemory != null) {
            vkFreeMemory(vkDevice, deviceImageMemory, null)
        }
    }
}