package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice

interface VulkanDeviceDestructionListener: PluginInterface {
    fun beforeDeviceDestruction(pluginInstance: PluginInstance, agent: BeforeAgent) {}

    fun afterDeviceDestruction(pluginInstance: PluginInstance, agent: AfterAgent) {}

    class BeforeAgent(
        val vulkanInstance: VkInstance,
        val vulkanPhysicalDevice: VkPhysicalDevice,
        val vulkanDevice: VkDevice
    )

    class AfterAgent(
        val vulkanInstance: VkInstance,
        val vulkanPhysicalDevice: VkPhysicalDevice
    )
}
