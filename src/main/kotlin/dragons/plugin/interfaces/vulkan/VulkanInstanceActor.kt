package dragons.plugin.interfaces.vulkan

import dragons.plugin.interfaces.PluginInterface

interface VulkanInstanceActor: PluginInterface {

    /**
     * Plug-ins that implement this method can influence the Vulkan instance extensions and layers that will be
     * enabled. See the documentation of the properties of *Agent* for more information.
     */
    fun manipulateVulkanInstance(agent: Agent)

    class Agent(
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
        val requiredExtensions: MutableSet<String>,

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
        val desiredLayers: Set<String>,
        /**
         * The set of layers that this plug-in requires to be enabled. These layers will be enabled if they are
         * available and the game will abort and show an error prompt if any layer in this set is not available.
         *
         * This set will be initially empty and the plug-in should add layers to this set during
         * manipulateVulkanInstance.
         */
        val requiredLayers: Set<String>
    )
}
