package dragons.plugin.interfaces.vulkan

import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance
import org.lwjgl.system.MemoryStack

interface VulkanInstanceActor: MagicPluginInterface {

    /**
     * Plug-ins that implement this method can influence the Vulkan (instance) layers that will be
     * enabled. See the documentation of the properties of *LayerAgent* for more information.
     */
    fun manipulateVulkanInstanceLayers(pluginInstance: PluginInstance, agent: LayerAgent) {}

    /**
     * Plug-ins that implement this method can influence the Vulkan instance extensions that will be
     * enabled. See the documentation of the properties of *ExtensionAgent* for more information.
     */
    fun manipulateVulkanInstanceExtensions(pluginInstance: PluginInstance, agent: ExtensionAgent) {}

    fun extendVulkanInstanceNextChain(pluginInstance: PluginInstance, agent: NextChainAgent) {}

    class ExtensionAgent(
        /**
         * The set of available Vulkan instance extensions (this will be queried using
         * vkEnumerateInstanceExtensionProperties).
         */
        val availableExtensions: Set<String>,
        /**
         * The set of extensions that this plug-in would like to enable, but doesn't require. These extensions will be
         * enabled if they are available and ignored if they are not available.
         *
         * This set will be initially empty and the plug-in should add extensions to this set during
         * manipulateVulkanInstance.
         */
        val desiredExtensions: MutableSet<String>,
        /**
         * The set of extensions that this plug-in requires to be enabled. These extensions will be enabled if they are
         * available and the game will abort and show an error prompt if any extension in this set is not available.
         *
         * This set will be initially empty and the plug-in should add extensions to this set during
         * manipulateVulkanInstance.
         */
        val requiredExtensions: MutableSet<String>
    )

    class LayerAgent(
        /**
         * The set of available Vulkan (instance) layers (this will be queried using vkEnumerateInstanceLayerProperties).
         */
        val availableLayers: Set<String>,

        /**
         * The set of layers that this plug-in would like to enable, but doesn't require. These layers will be
         * enabled if they are available and ignored if they are not available.
         *
         * This set will be initially empty and the plug-in should add layers to this set during
         * manipulateVulkanInstance.
         */
        val desiredLayers: MutableSet<String>,

        /**
         * The set of layers that this plug-in requires to be enabled. These layers will be enabled if they are
         * available and the game will abort and show an error prompt if any layer in this set is not available.
         *
         * This set will be initially empty and the plug-in should add layers to this set during
         * manipulateVulkanInstance.
         */
        val requiredLayers: MutableSet<String>
    )

    class NextChainAgent(
            val enabledLayers: Set<String>,
            val enabledExtensions: Set<String>,
            val stack: MemoryStack,
            var pNext: Long
    )
}
