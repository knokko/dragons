package dragons.vr

import org.lwjgl.vulkan.VkPhysicalDevice

class DummyVrManager(
    val instanceExtensions: Set<String> = setOf(),
    val deviceExtensions: Set<String> = setOf()
): VrManager {
    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        return instanceExtensions
    }

    override fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String> {
        return deviceExtensions
    }

    // These values are pretty arbitrary, but some decision had to be made
    override fun getWidth() = 1600
    override fun getHeight() = 900

    override fun destroy() {}
}
