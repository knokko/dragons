package dragons.vulkan.memory

import org.lwjgl.vulkan.VK12.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties

class MemoryInfo(
    device: VkPhysicalDevice,
) {

    // The create() method will cause it to be garbage collected, which is suitable in this case
    val memoryProperties: VkPhysicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.create()

    init {
        vkGetPhysicalDeviceMemoryProperties(device, memoryProperties)
    }

    fun chooseMemoryTypeIndex(requiredMemoryTypeBits: Int, requiredPropertyFlags: Int): Int? {
        for ((memoryTypeIndex, memoryType) in memoryProperties.memoryTypes().withIndex()) {
            // Only consider memory types that are allowed
            if ((memoryTypeIndex and requiredMemoryTypeBits) == requiredMemoryTypeBits) {
                // From the allowed memory types, pick the first one that has all the required properties
                if ((requiredPropertyFlags and memoryType.propertyFlags()) == requiredPropertyFlags) {
                    return memoryTypeIndex
                }
            }
        }
        return null
    }
}
