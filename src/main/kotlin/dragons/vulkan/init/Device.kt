package dragons.vulkan.init

import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanDeviceRater
import dragons.util.assertVkSuccess
import dragons.vr.VrManager
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger
import java.nio.ByteBuffer
import kotlin.jvm.Throws

@Throws(StartupException::class)
fun choosePhysicalDevice(vkInstance: VkInstance, pluginManager: PluginManager, vrManager: VrManager): VkPhysicalDevice {
    val logger = getLogger("Vulkan")

    return stackPush().use { stack ->
        val pNumDevices = stack.callocInt(1)
        assertVkSuccess(
            vkEnumeratePhysicalDevices(vkInstance, pNumDevices, null),
            "EnumeratePhysicalDevices", "count"
        )
        val numDevices = pNumDevices[0]
        logger.info("There are $numDevices physical devices with Vulkan support")

        val pPhysicalDevices = stack.callocPointer(numDevices)
        assertVkSuccess(
            vkEnumeratePhysicalDevices(vkInstance, pNumDevices, pPhysicalDevices),
            "EnumeratePhysicalDevices", "device pointers"
        )

        val deviceRaters = pluginManager.getImplementations(VulkanDeviceRater::class)
        val deviceNames = Array(numDevices) { "" }

        val deviceRatings = (0 until numDevices).map { deviceIndex ->
            val physicalDevice = VkPhysicalDevice(pPhysicalDevices[deviceIndex], vkInstance)

            val properties = VkPhysicalDeviceProperties2.callocStack(stack)
            properties.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)

            val properties10 = properties.properties()

            val properties11 = VkPhysicalDeviceVulkan11Properties.callocStack(stack)
            properties11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES)

            val properties12 = VkPhysicalDeviceVulkan12Properties.callocStack(stack)
            properties12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_PROPERTIES)

            properties.pNext(properties11.address())
            properties11.pNext(properties12.address())

            vkGetPhysicalDeviceProperties2(physicalDevice, properties)

            val deviceName = properties.properties().deviceNameString()
            deviceNames[deviceIndex] = deviceName

            val vulkanVersion = properties.properties().apiVersion()
            val vulkanVersionString =
                "${VK_VERSION_MAJOR(vulkanVersion)}.${VK_VERSION_MINOR(vulkanVersion)}.${VK_VERSION_PATCH(vulkanVersion)}"

            logger.info("Device $deviceIndex is $deviceName and supports up to Vulkan $vulkanVersionString")
            if (vulkanVersion < VK_MAKE_VERSION(1, 2, 0)) {
                logger.warn("$deviceName doesn't support Vulkan 1.2.0")
                return@map DeviceRating(null,
                    DeviceRejectionReason("the game", VulkanDeviceRater.InsufficientReason.other(
                        "it doesn't support Vulkan 1.2.0 (it supports only up to $vulkanVersionString)"
                    ))
                )
            }

            val pNumExtensions = stack.callocInt(1)
            assertVkSuccess(
                vkEnumerateDeviceExtensionProperties(physicalDevice, null as ByteBuffer?, pNumExtensions, null),
                "EnumerateDeviceExtensionProperties", "count for $deviceName"
            )
            val numExtensions = pNumExtensions[0]

            val pExtensions = VkExtensionProperties.callocStack(numExtensions, stack)
            assertVkSuccess(
                vkEnumerateDeviceExtensionProperties(physicalDevice, null as ByteBuffer?, pNumExtensions, pExtensions),
                "EnumerateDeviceExtensionProperties", "extensions for $deviceName"
            )

            val availableExtensions = mutableSetOf<String>()
            for (extensionIndex in 0 until numExtensions) {
                availableExtensions.add(pExtensions[extensionIndex].extensionNameString())
            }

            logger.info("$deviceName supports $numExtensions device extensions:")
            for (extension in availableExtensions) {
                logger.info(extension)
            }

            val requiredVrExtensions = vrManager.getVulkanDeviceExtensions(physicalDevice, deviceName, availableExtensions)
            for (extension in requiredVrExtensions) {
                if (!availableExtensions.contains(extension)) {
                    logger.warn("$deviceName misses the extension $extension that is required by the VR manager")
                    return@map DeviceRating(null, DeviceRejectionReason(
                        "the VR manager",
                        VulkanDeviceRater.InsufficientReason.missesRequiredExtension(extension)
                    ))
                }
            }

            val availableFeatures = VkPhysicalDeviceFeatures2.callocStack(stack)
            availableFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)

            val availableFeatures10 = availableFeatures.features()

            val availableFeatures11 = VkPhysicalDeviceVulkan11Features.callocStack(stack)
            availableFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)

            val availableFeatures12 = VkPhysicalDeviceVulkan12Features.callocStack(stack)
            availableFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)

            availableFeatures.pNext(availableFeatures11.address())
            availableFeatures11.pNext(availableFeatures12.address())

            vkGetPhysicalDeviceFeatures2(physicalDevice, availableFeatures)

            val memoryProperties = VkPhysicalDeviceMemoryProperties.callocStack(stack)
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties)

            val pNumQueueFamilies = stack.callocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pNumQueueFamilies, null)
            val numQueueFamilies = pNumQueueFamilies[0]

            val queueFamilyProperties = VkQueueFamilyProperties.callocStack(numQueueFamilies, stack)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pNumQueueFamilies, queueFamilyProperties)

            val deviceScores = deviceRaters.map { raterPair ->
                val pluginAgent = VulkanDeviceRater.Agent(
                    vkInstance, physicalDevice, availableExtensions,
                    properties10, properties11, properties12,
                    availableFeatures10, availableFeatures11, availableFeatures12,
                    memoryProperties, queueFamilyProperties
                )
                raterPair.first.ratePhysicalDevice(raterPair.second, pluginAgent)
                Pair(pluginAgent.rating, raterPair.second)
            }.filter { scorePair -> scorePair.first != null }.map { scorePair -> Pair(scorePair.first!!, scorePair.second) }

            for ((rating, plugin) in deviceScores) {
                logger.info("Plug-in ${plugin.info.name} gave the following rating to $deviceName: $rating")
            }

            for ((rating, plugin) in deviceScores) {
                if (!rating.isSufficient()) {
                    logger.warn("$deviceName is considered insufficient for plug-in ${plugin.info.name}")
                    return@map DeviceRating(null, DeviceRejectionReason(
                        "the ${plugin.info.name} plug-in",
                        rating.getRejectionReason()
                    ))
                }
            }

            var score = 0u
            for ((rating, _) in deviceScores) {
                score += rating.getScore()
            }

            logger.info("The final score of $deviceName is $score")

            DeviceRating(score, null)
        }

        val bestDeviceIndex = (0 until numDevices).filter {
                deviceIndex -> deviceRatings[deviceIndex].score != null
        }.maxByOrNull { deviceIndex -> deviceRatings[deviceIndex].score!! }

        if (bestDeviceIndex != null) {
            logger.info("Chose physical device ${deviceNames[bestDeviceIndex]} ($bestDeviceIndex)")
            VkPhysicalDevice(pPhysicalDevices[bestDeviceIndex], vkInstance)
        } else {
            logger.error("Not a single physical device is sufficient")
            val errorDescription = mutableListOf(
                "None of the graphics cards of your computer are good enough to play this game (with these plug-ins).",
                "Your computer has $numDevices graphics cards:"
            )
            for ((index, rating) in deviceRatings.withIndex()) {
                errorDescription.add("${deviceNames[index]} is insufficient ${rating.rejectionReason}")
            }
            throw SimpleStartupException("No suitable graphics card found", errorDescription)
        }
    }
}

private class DeviceRating(val score: UInt?, val rejectionReason: DeviceRejectionReason?)

private class DeviceRejectionReason(val rejectingEntity: String, val rejectionReason: VulkanDeviceRater.InsufficientReason) {
    override fun toString(): String {
        return "for $rejectingEntity because $rejectionReason"
    }
}
