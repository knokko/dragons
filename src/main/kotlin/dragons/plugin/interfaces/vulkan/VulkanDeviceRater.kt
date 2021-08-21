package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import org.lwjgl.vulkan.*
import kotlin.jvm.Throws

interface VulkanDeviceRater: PluginInterface {
    /**
     * Plug-ins can implement this method to give a rating to *agent.vulkanDevice*, or even forbid the game for
     * choosing this device. To do so, assign a *Rating* to *agent.rating*.
     *
     * **Note**
     *  - This method will be called for each physical device that supports Vulkan 1.2, even if another *VulkanDeviceRater*
     * has already rejected the device.
     *  - The *agent* has quite some information about *vulkanDevice* (like its properties and features). Using these
     *  rather than querying them yourself is recommended because it avoids the need to query them **for each** plug-in.
     *  Also, it should be slightly less work to program. **But, they are expected to be allocated on a stack that will
     *  be popped after all *VulkanDeviceRater*s are finished, so do *NOT* store these properties for later!** (Unless
     *  you copy them.)
     *  - Plug-ins can use as many *VulkanDeviceRater* as they want (within reason; expect memory problems if you have
     *  more than a million).
     *
     * **Selection procedure**
     *
     * Before the *ratePhysicalDevice* methods of the plug-ins are called, the game will query all physical devices
     * using *vkEnumeratePhysicalDevices* and eliminate all devices that don't support Vulkan 1.2. Also, the *VRManager*
     * is allowed to reject devices. Finally, the game itself can reject devices.
     *
     * Then, the game will call *ratePhysicalDevice* for each remaining physical device for each *VulkanDeviceRater*.
     * All devices that are **rejected** by at least 1 *VulkanDeviceRater* will be eliminated.
     *
     * If no devices are left, a notification window will be opened that tells the user why each device was rejected,
     * and by which plug-in (if applicable).
     *
     * For each remaining physical device, the scores given by each *VulkanDeviceRater* will be summed up. If needed,
     * the scores will be capped to *UInt.MAX_VALUE*. The game will choose a physical device with the highest score.
     * If multiple devices have the highest score, one of them will be picked (it is an implementation detail which one).
     */
    fun ratePhysicalDevice(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val vulkanInstance: VkInstance,
        /**
         * The Vulkan physical device that should be rated
         */
        val vulkanDevice: VkPhysicalDevice,
        /**
         * A set containing all device extensions supported by *vulkanDevice*
         */
        val availableExtensions: Set<String>,
        val properties10: VkPhysicalDeviceProperties,
        val properties11: VkPhysicalDeviceVulkan11Properties,
        val properties12: VkPhysicalDeviceVulkan12Properties,
        /** The supported Vulkan 1.0 features */
        val availableFeatures10: VkPhysicalDeviceFeatures,
        /** The supported Vulkan 1.1 features */
        val availableFeatures11: VkPhysicalDeviceVulkan11Features,
        /** The supported Vulkan 1.2 features */
        val availableFeatures12: VkPhysicalDeviceVulkan12Features,
        val memoryProperties: VkPhysicalDeviceMemoryProperties,
        /** The queue family properties of each queue of the physical device */
        val queueFamilyProperties: VkQueueFamilyProperties.Buffer,
        /**
         * The rating that this plug-in gives to *vulkanDevice*.
         *
         * This will be null by default, but the plug-in can change it if it wants to give a rating.
         */
        var rating: Rating? = null
    )

    class Rating private constructor(private val score: UInt?, private val reason: InsufficientReason?) {

        /**
         * Whether this Rating is *sufficient*, or not. If this Rating is insufficient, the corresponding physical
         * device must not be chosen.
         */
        fun isSufficient() = score != null

        /**
         * If this *Rating* is **sufficient**, the given score will be returned.
         *
         * If this *Rating* is **insufficient**, an *UnsupportedOperationException* will be thrown
         */
        @Throws(UnsupportedOperationException::class)
        fun getScore() = score?: throw UnsupportedOperationException("This rating is insufficient")

        /**
         * If this *Rating* is **sufficient**, an *UnsupportedOperationException* will be thrown.
         *
         * If this *Rating* is **insufficient**, the reason will be returned.
         */
        @Throws(UnsupportedOperationException::class)
        fun getRejectionReason() = reason?: throw UnsupportedOperationException("The rating is sufficient")

        override fun toString(): String {
            return if (isSufficient()) {
                "$score"
            } else {
                "insufficient because $reason"
            }
        }

        companion object {
            /**
             * Considers the physical device *sufficient* and assigns it a score.
             */
            fun sufficient(score: UInt) = Rating(score, null)

            /**
             * Giving this Rating forbids the game from choosing this physical device. This should only be used if the
             * plug-in really can't work with the corresponding physical device. If all physical devices are deemed
             * *insufficient*, the game won't be able to start.
             */
            fun insufficient(reason: InsufficientReason) = Rating(null, reason)
        }
    }

    class InsufficientReason private constructor(
        val reason: String,
        val missingExtension: String? = null,
        val missingFeature: String? = null
    ) {
        override fun toString() = reason
        companion object {
            fun other(reason: String) = InsufficientReason(reason = reason)

            fun missesRequiredExtension(extensionName: String) = InsufficientReason(
                reason = "it misses the required extension $extensionName",
                missingExtension = extensionName
            )

            fun missesRequiredFeature(featureName: String) = InsufficientReason(
                reason = "it misses the required feature $featureName",
                missingFeature = featureName
            )
        }
    }
}
