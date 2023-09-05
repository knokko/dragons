package dragons.plugin.interfaces.vulkan

import knokko.plugin.MagicPluginInterface
import knokko.plugin.PluginInstance
import org.lwjgl.vulkan.*

interface VulkanDeviceActor: MagicPluginInterface {
    /**
     * Plug-ins can implement this method to influence the logical device creation process: they can request and require
     * device extensions and device features, as well as add structures to the pNext chain.
     *
     * **Note**
     *  - All feature fields and pNext chain fields of the agent are expected to be allocated on a memory stack that will
     * be dropped after all calls to `manipulateVulkanDevice` are finished. This means that plug-ins can freely read
     * or modify them **during** the call to `manipulateVulkanDevice`, but **not afterwards**.
     *  - Plug-ins are expected to use the `availableExtensions`, `availableFeatures10`, ... of the *agent* rather than
     *  querying these properties from the physical device. Querying them is allowed, but requires writing more code and
     *  is inefficient (since it requires an extra Vulkan query).
     */
    fun manipulateVulkanDevice(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        /** The Vulkan instance of the game */
        val vulkanInstance: VkInstance,
        /** The Vulkan physical device for which the logical device will be created */
        val physicalDevice: VkPhysicalDevice,

        /**
         * The set of device extensions that are supported by `physicalDevice`
         * (queried using `vkEnumerateDeviceExtensionProperties`)
         */
        val availableExtensions: Set<String>,
        /**
         * The set of device extensions that this plug-in would like to enable, but are not *required*. The plug-in
         * can request device extensions by adding them to this set.
         *
         * Adding an available extension to this set will guarantee that the extension will be enabled. Adding an
         * unavailable extension to this set has no effect.
         *
         * This set will be empty at the start of the call to `manipulateVulkanDevice`.
         */
        val requestedExtensions: MutableSet<String>,
        /**
         * The set of device extensions that this plug-in requires to be enabled. The plug-in can require device
         * extensions by adding them to this set.
         *
         * Adding an available extension to this set will guarantee that the extension will be enabled. Adding an
         * unavailable extension to this set will abort the game.
         *
         * This set will be empty at the start of the call to `manipulateVulkanDevice`.
         */
        val requiredExtensions: MutableSet<String>,

        /**
         * The Vulkan 1.0 device features that are supported by `physicalDevice`. This will be queried using
         * `vkGetPhysicalDeviceFeatures`.
         */
        val availableFeatures: VkPhysicalDeviceFeatures,

        /**
         * The Vulkan 1.0 device features that this plug-in would like to enable, but are not *required*. The plug-in
         * can request device features by calling their corresponding methods on `requestedFeatures` with `true` as
         * only parameter. (Something like `agent.requestedFeatures.featureName(true)`)
         *
         * Adding an available feature will guarantee that the feature will be enabled. Adding an unavailable feature
         * has no effect.
         *
         * All features will be false at the start of the call to `manipulateVulkanDevice`.
         */
        val requestedFeatures: VkPhysicalDeviceFeatures,

        /**
         * The Vulkan 1.0 device features that this plug-in **requires** to work. The plug-in can require device
         * features by calling their corresponding methods on `requiredFeatures` with `true` as only parameter.
         * (Something like `agent.requiredFeatures.featureName(true)`)
         *
         * Adding an available feature will guarantee that the feature will be enabled. Adding an unavailable feature
         * will abort the game.
         *
         * All features will be false at the start of the call to `manipulateVulkanDevice`.
         */
        val requiredFeatures: VkPhysicalDeviceFeatures,

        /**
         * The current `pNext` chain. This chain will contain all the structures added by the plug-ins whose
         * `manipulateVulkanDevice` methods were called earlier. This chain will **not** contain a
         * `VkPhysicalDeviceVulkan11Features` or `VkPhysicalDeviceVulkan12Features` structs: these will be added
         * implicitly by the game. Plug-ins must **not** add these structs themselves, or the device creation will fail!
         */
        val currentNextChain: VkBaseOutStructure?,
        /**
         * The structures that this plug-in would like to add to the `pNext` chain. If a plug-in would like to add 1
         * or more structures to the `pNext` chain, they should create their chain and set this variable to the
         * first structure in their chain.
         *
         * **Note**
         *
         * To avoid having duplicate structures in the chain (which is generally forbidden), plug-ins should first check
         * `currentNextChain` to see if another plug-in already added the structure. Furthermore, plug-ins should
         * **not** add `VkPhysicalDeviceVulkan11Features` or `VkPhysicalDeviceVulkan12Features` because the game will
         * add them implicitly.
         */
        var extendNextChain: VkBaseOutStructure? = null
    )
}
