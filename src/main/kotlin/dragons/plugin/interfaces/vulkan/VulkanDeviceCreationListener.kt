package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.vr.VrManager
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.vulkan.*

interface VulkanDeviceCreationListener: PluginInterface {
    /**
     * This method will be called soon after the game has created its logical device.
     */
    fun afterVulkanDeviceCreation(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        /** The Vulkan instance of the game */
        val vulkanInstance: VkInstance,
        /** The Vulkan physical device for which the logical device was created */
        val vulkanPhysicalDevice: VkPhysicalDevice,
        /** The logical device that was just created */
        val vulkanDevice: VkDevice,
        /** The *RenderImageInfo* corresponding to *vulkanPhysicalDevice* */
        val renderImageInfo: RenderImageInfo,
        /** The set of device extensions that were enabled */
        val enabledExtensions: Set<String>,
        /** The Vulkan 1.0 device features that were enabled */
        val enabledFeatures: VkPhysicalDeviceFeatures,
        /** The `QueueManager` of the game. Plug-ins can use this to retrieve the device queues they want. */
        val queueManager: QueueManager,
        /** The `VrManager` of the game. */
        val vrManager: VrManager,
        /** The `CoroutineScope` for the initialization of the game (until the main menu is shown)*/
        val gameInitScope: CoroutineScope
    )
}