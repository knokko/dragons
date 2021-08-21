package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

interface VulkanInstanceCreationListener: PluginInterface {
    /**
     * This method will be called right after the game created its Vulkan instance. Plug-ins can use this method to
     * create Vulkan resources that need (only) a Vulkan instance.
     */
    fun afterVulkanInstanceCreation(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        /**
         * The *VkInstance* that was created
         */
        val vulkanInstance: VkInstance,
        /**
         * The set of (instance) layers that were enabled
         */
        val enabledLayers: Set<String>,
        /**
         * The set of instance extensions that were enabled
         */
        val enabledExtensions: Set<String>,
        /**
         * The *VkInstanceCreateInfo* that was used to create *vulkanInstance*. NOTE: This struct is expected to be
         * allocated on a *MemoryStack* and will probably be invalidated after all *VulkanInstanceCreationListener*s
         * have been notified.
         *
         * This means that all listeners can examine this struct **during** their call to *afterVulkanInstanceCreation*,
         * but should NOT store the struct and attempt to read it afterwards.
         */
        val ciInstance: VkInstanceCreateInfo
    )
}
