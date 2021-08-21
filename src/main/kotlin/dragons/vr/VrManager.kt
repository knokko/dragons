package dragons.vr

import org.lwjgl.vulkan.VkPhysicalDevice

interface VrManager {
    fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String>

    fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String>

    fun destroy()
}
