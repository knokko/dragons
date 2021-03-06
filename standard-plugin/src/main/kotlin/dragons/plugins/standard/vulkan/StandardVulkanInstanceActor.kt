package dragons.plugins.standard.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanInstanceActor
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME

class StandardVulkanInstanceActor: VulkanInstanceActor {
    override fun manipulateVulkanInstance(pluginInstance: PluginInstance, agent: VulkanInstanceActor.Agent) {
        // This extension is required by VK_EXT_memory_budget, which helps the Vulkan Memory Allocator (VMA)
        agent.desiredExtensions.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME)
    }
}
