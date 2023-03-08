package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanInstanceActor
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME

@Suppress("unused")
class StandardVulkanInstanceActor: VulkanInstanceActor {
    override fun manipulateVulkanInstanceExtensions(pluginInstance: PluginInstance, agent: VulkanInstanceActor.ExtensionAgent) {
        // This extension is required by VK_EXT_memory_budget, which helps the Vulkan Memory Allocator (VMA)
        agent.desiredExtensions.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME)
    }
}
