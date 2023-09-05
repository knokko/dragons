package dragons.plugin.interfaces.vulkan

import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice

interface VulkanDeviceDestructionListener: MagicPluginInterface {
    /**
     * This method will be called soon before the game will destroy its logical device.
     */
    fun beforeDeviceDestruction(pluginInstance: PluginInstance, agent: BeforeAgent) {}

    /**
     * This method will be called soon after the game has destroyed its logical device.
     */
    fun afterDeviceDestruction(pluginInstance: PluginInstance, agent: AfterAgent) {}

    class BeforeAgent(
        /** The Vulkan instance of the game */
        val vulkanInstance: VkInstance,
        /** The Vulkan physical device for which the logical device was created */
        val vulkanPhysicalDevice: VkPhysicalDevice,
        /** The Vulkan logical device that is about to be destroyed */
        val vulkanDevice: VkDevice
    )

    class AfterAgent(
        /** The Vulkan instance of the game */
        val vulkanInstance: VkInstance,
        /** The Vulkan physical device for which the destroyed logical device was created */
        val vulkanPhysicalDevice: VkPhysicalDevice
    )
}
