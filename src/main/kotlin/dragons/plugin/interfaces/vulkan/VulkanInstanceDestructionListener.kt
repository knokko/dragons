package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import org.lwjgl.vulkan.VkInstance

interface VulkanInstanceDestructionListener: PluginInterface {
    /**
     * This method will be called right before the game will destroy its Vulkan instance. Plug-ins can use this method
     * to clean up Vulkan resources that are related to the Vulkan instance.
     */
    fun beforeInstanceDestruction(pluginInstance: PluginInstance, agent: BeforeAgent) {}

    /**
     * This method will be called right after the game destroyed its Vulkan instance.
     */
    fun afterInstanceDestruction(pluginInstance: PluginInstance, agent: AfterAgent) {}

    class BeforeAgent(
        /**
         * The *VkInstance* that is about to be destroyed.
         */
        val vulkanInstance: VkInstance
    )

    /**
     * This agent is currently empty (because I can't think of any useful properties to add), but this might change in
     * the future.
     */
    class AfterAgent
}
