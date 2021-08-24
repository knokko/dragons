package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import org.lwjgl.vulkan.*

interface VulkanDeviceActor: PluginInterface {
    fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val vulkanInstance: VkInstance,
        val physicalDevice: VkPhysicalDevice,

        val availableExtensions: Set<String>,
        val requestedExtensions: MutableSet<String>,
        val requiredExtensions: MutableSet<String>,

        val availableFeatures10: VkPhysicalDeviceFeatures,
        val availableFeatures11: VkPhysicalDeviceVulkan11Features,
        val availableFeatures12: VkPhysicalDeviceVulkan12Features,

        val featuresToEnable10: VkPhysicalDeviceFeatures,
        val featuresToEnable11: VkPhysicalDeviceVulkan11Features,
        val featuresToEnable12: VkPhysicalDeviceVulkan12Features,

        val currentNextChain: VkBaseOutStructure?,
        var extendNextChain: VkBaseOutStructure? = null
    )
}
