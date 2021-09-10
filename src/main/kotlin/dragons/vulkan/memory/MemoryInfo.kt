package dragons.vulkan.memory

import org.lwjgl.vulkan.VK12.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties

class MemoryInfo(
    val memoryProperties: VkPhysicalDeviceMemoryProperties
) {

    // The create() method will cause it to be garbage collected, which is suitable in this case
    constructor(device: VkPhysicalDevice) : this(VkPhysicalDeviceMemoryProperties.create()) {
        vkGetPhysicalDeviceMemoryProperties(device, memoryProperties)
    }

    fun chooseMemoryTypeIndex(
        allowedMemoryTypeBits: Int,
        requiredSize: Long,
        requiredPropertyFlags: Int = 0,
        desiredPropertyFlags: Int = 0,
        neutralPropertyFlags: Int = 0
    ): Int? {
        val candidates = memoryProperties.memoryTypes().withIndex().filter { (memoryTypeIndex, memoryType) ->
            // Only consider memory types that are allowed
            if (((1 shl memoryTypeIndex) and allowedMemoryTypeBits) != 0) {
                // Only consider memory types that have all required properties
                if ((requiredPropertyFlags and memoryType.propertyFlags()) == requiredPropertyFlags) {
                    // And those that are sufficiently big
                    if (memoryProperties.memoryHeaps(memoryType.heapIndex()).size() >= requiredSize) {
                        return@filter true
                    }
                }
            }
            return@filter false
        }

        return candidates.maxByOrNull { (_, type) ->
            val positiveScore = 1000 * Integer.bitCount(type.propertyFlags() and desiredPropertyFlags)
            val innocentPropertyFlags = requiredPropertyFlags or desiredPropertyFlags or neutralPropertyFlags
            val undesiredPropertyFlags = innocentPropertyFlags.inv()
            val negativeScore = Integer.bitCount(type.propertyFlags() and undesiredPropertyFlags)
            positiveScore - negativeScore
        }?.index
    }
}
