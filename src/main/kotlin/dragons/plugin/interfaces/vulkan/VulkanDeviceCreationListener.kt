package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.vulkan.queue.QueueManager
import org.lwjgl.vulkan.*

interface VulkanDeviceCreationListener: PluginInterface {
    fun afterVulkanDeviceCreation(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val vulkanInstance: VkInstance,
        val vulkanPhysicalDevice: VkPhysicalDevice,
        val vulkanDevice: VkDevice,
        val enabledExtensions: Set<String>,
        val enabledFeatures10: VkPhysicalDeviceFeatures,
        val enabledFeatures11: VkPhysicalDeviceVulkan11Features,
        val enabledFeatures12: VkPhysicalDeviceVulkan12Features,
        val queueManager: QueueManager
    )
}