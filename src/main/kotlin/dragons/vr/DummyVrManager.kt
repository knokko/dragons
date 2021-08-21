package dragons.vr

import org.lwjgl.vulkan.VkPhysicalDevice

class DummyVrManager: VrManager {
    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        return setOf()
    }

    override fun getVulkanDeviceExtensions(
        device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>
    ): Set<String> {
        return setOf()
    }

    override fun destroy() {}
}
