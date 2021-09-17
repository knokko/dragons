package dragons.vulkan.init

import dragons.init.trouble.ExtensionStartupException
import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanDeviceActor
import dragons.plugin.interfaces.vulkan.VulkanDeviceCreationListener
import dragons.plugin.interfaces.vulkan.VulkanDeviceRater
import dragons.vr.VrManager
import dragons.vulkan.queue.DeviceQueue
import dragons.vulkan.queue.QueueFamily
import dragons.vulkan.queue.QueueManager
import dragons.vulkan.util.assertVkSuccess
import dragons.vulkan.util.combineNextChains
import dragons.vulkan.util.encodeStrings
import dragons.vulkan.util.extensionBufferToSet
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memGetInt
import org.lwjgl.system.MemoryUtil.memPutInt
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.jvm.Throws
import kotlin.math.exp
import kotlin.math.log

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

            val properties = VkPhysicalDeviceProperties2.calloc(stack)
            properties.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)

            val properties10 = properties.properties()

            val properties11 = VkPhysicalDeviceVulkan11Properties.calloc(stack)
            properties11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES)

            val properties12 = VkPhysicalDeviceVulkan12Properties.calloc(stack)
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

            val pExtensions = VkExtensionProperties.calloc(numExtensions, stack)
            assertVkSuccess(
                vkEnumerateDeviceExtensionProperties(physicalDevice, null as ByteBuffer?, pNumExtensions, pExtensions),
                "EnumerateDeviceExtensionProperties", "extensions for $deviceName"
            )

            val availableExtensions = extensionBufferToSet(pExtensions)

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

            val availableFeatures = VkPhysicalDeviceFeatures2.calloc(stack)
            availableFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)

            val availableFeatures10 = availableFeatures.features()

            val availableFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
            availableFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)

            val availableFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
            availableFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)

            availableFeatures.pNext(availableFeatures11.address())
            availableFeatures11.pNext(availableFeatures12.address())

            vkGetPhysicalDeviceFeatures2(physicalDevice, availableFeatures)

            val memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties)

            val pNumQueueFamilies = stack.callocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pNumQueueFamilies, null)
            val numQueueFamilies = pNumQueueFamilies[0]

            val queueFamilyProperties = VkQueueFamilyProperties.calloc(numQueueFamilies, stack)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pNumQueueFamilies, queueFamilyProperties)

            // Note: (almost) any real physical device has such a queue family, but it's always better to check than assume
            if (
                !queueFamilyProperties.any { queueFamily ->
                    (queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0 && (queueFamily.queueFlags() and VK_QUEUE_COMPUTE_BIT) != 0
                }) {
                return@map DeviceRating(
                    null, DeviceRejectionReason("the game", VulkanDeviceRater.InsufficientReason.other(
                            "misses a queue family with support for both graphics and compute operations"
                        )
                    )
                )
            }

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

@Throws(StartupException::class)
fun createLogicalDevice(
    vkInstance: VkInstance, physicalDevice: VkPhysicalDevice,
    pluginManager: PluginManager, vrManager: VrManager
): Pair<VkDevice, QueueManager> {
    val logger = getLogger("Vulkan")
    return stackPush().use { stack ->

        val pNumAvailableExtensions = stack.callocInt(1)
        assertVkSuccess(
            vkEnumerateDeviceExtensionProperties(physicalDevice, null as ByteBuffer?, pNumAvailableExtensions, null),
            "EnumerateDeviceExtensionProperties", "count"
        )
        val numAvailableExtensions = pNumAvailableExtensions[0]

        val pAvailableExtensions = VkExtensionProperties.calloc(numAvailableExtensions, stack)
        assertVkSuccess(
            vkEnumerateDeviceExtensionProperties(physicalDevice, null as ByteBuffer?, pNumAvailableExtensions, pAvailableExtensions),
            "EnumerateDeviceExtensionProperties", "extensions"
        )

        val availableExtensions = extensionBufferToSet(pAvailableExtensions)


        val availableFeatures = VkPhysicalDeviceFeatures2.calloc(stack)
        availableFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)

        val availableFeatures10 = availableFeatures.features()

        val availableFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
        availableFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)

        val availableFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
        availableFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)

        availableFeatures.pNext(availableFeatures11.address())
        availableFeatures11.pNext(availableFeatures12.address())

        vkGetPhysicalDeviceFeatures2(physicalDevice, availableFeatures)

        logger.info("The following Vulkan 1.0 device features are supported:")
        for (feature in getEnabledFeatures(availableFeatures10)) {
            logger.info(feature)
        }
        logger.info("The following Vulkan 1.1 device features are supported:")
        for (feature in getEnabledFeatures(availableFeatures11)) {
            logger.info(feature)
        }
        logger.info("The following Vulkan 1.2 device features are supported:")
        for (feature in getEnabledFeatures(availableFeatures12)) {
            logger.info(feature)
        }

        val pNumQueueFamilies = stack.callocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pNumQueueFamilies, null)
        val numQueueFamilies = pNumQueueFamilies[0]

        val pQueueFamilies = VkQueueFamilyProperties.calloc(numQueueFamilies, stack)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pNumQueueFamilies, pQueueFamilies)

        val ciDevice = VkDeviceCreateInfo.calloc(stack)
        val populateResult = populateDeviceCreateInfo(
            ciDevice, vkInstance, physicalDevice, stack, pluginManager, vrManager, availableExtensions,
            pQueueFamilies, availableFeatures10, availableFeatures11, availableFeatures12
        )

        logger.info("The following Vulkan device extensions will be enabled:")
        for (extension in populateResult.enabledExtensions) {
            logger.info(extension)
        }
        logger.info("The following Vulkan 1.0 device features will be enabled:")
        for (feature in getEnabledFeatures(populateResult.enabledFeatures10)) {
            logger.info(feature)
        }
        logger.info("The following Vulkan 1.1 device features will be enabled:")
        for (feature in getEnabledFeatures(populateResult.enabledFeatures11)) {
            logger.info(feature)
        }
        logger.info("The following Vulkan 1.2 device features will be enabled:")
        for (feature in getEnabledFeatures(populateResult.enabledFeatures12)) {
            logger.info(feature)
        }

        logger.info(
            "The game will use ${populateResult.generalQueueFamily.numPriorityQueues} priority queues " +
                    "and ${populateResult.generalQueueFamily.numBackgroundQueues} background queues"
        )
        if (populateResult.computeOnlyQueueFamily != null) {
            logger.info(
                "The game will use ${populateResult.computeOnlyQueueFamily.numPriorityQueues} priority compute-only queues" +
                        " and ${populateResult.computeOnlyQueueFamily.numBackgroundQueues} background compute-only queues"
            )
        }
        if (populateResult.transferOnlyQueueFamily != null) {
            logger.info(
                "The game will use ${populateResult.transferOnlyQueueFamily.numPriorityQueues} priority transfer-only queues" +
                        " and ${populateResult.transferOnlyQueueFamily.numBackgroundQueues} background transfer-only queues"
            )
        }

        logger.info("Creating logical device...")
        val pDevice = stack.callocPointer(1)
        assertVkSuccess(
            vkCreateDevice(physicalDevice, ciDevice, null, pDevice),
            "CreateDevice"
        )
        val device = VkDevice(pDevice[0], physicalDevice, ciDevice)
        logger.info("Created logical device")

        fun retrieveQueues(info: QueueFamilyInfo): QueueFamily {
            val priorityQueues = (0 until info.numPriorityQueues).map { queueIndex ->
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(device, info.index, queueIndex, pQueue)
                DeviceQueue(VkQueue(pQueue[0], device))
            }
            val backgroundQueues = (0 until info.numBackgroundQueues).map { partialQueueIndex ->
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(device, info.index, partialQueueIndex + info.numPriorityQueues, pQueue)
                DeviceQueue(VkQueue(pQueue[0], device))
            }
            return QueueFamily(priorityQueues = priorityQueues, backgroundQueues = backgroundQueues, index = info.index)
        }

        fun maybeRetrieveQueues(info: QueueFamilyInfo?) = if (info != null) { retrieveQueues(info) } else { null }

        val queueManager = QueueManager(
            retrieveQueues(populateResult.generalQueueFamily),
            maybeRetrieveQueues(populateResult.computeOnlyQueueFamily),
            maybeRetrieveQueues(populateResult.transferOnlyQueueFamily)
        )
        logger.info("Retrieved device queues")

        val eventAgent = VulkanDeviceCreationListener.Agent(
            vkInstance, physicalDevice, device,
            populateResult.enabledExtensions, populateResult.enabledFeatures10,
            populateResult.enabledFeatures11, populateResult.enabledFeatures12,
            queueManager
        )
        pluginManager.getImplementations(VulkanDeviceCreationListener::class).forEach { pluginPair ->
            pluginPair.first.afterVulkanDeviceCreation(pluginPair.second, eventAgent)
        }

        Pair(device, queueManager)
    }
}

@Throws(StartupException::class)
internal fun populateDeviceCreateInfo(
    ciDevice: VkDeviceCreateInfo, vkInstance: VkInstance, physicalDevice: VkPhysicalDevice, stack: MemoryStack,
    pluginManager: PluginManager, vrManager: VrManager,
    availableExtensions: Set<String>, queueFamilies: VkQueueFamilyProperties.Buffer,
    availableFeatures10: VkPhysicalDeviceFeatures,
    availableFeatures11: VkPhysicalDeviceVulkan11Features, availableFeatures12: VkPhysicalDeviceVulkan12Features
): PopulateDeviceResult {
    val logger = getLogger("Vulkan")

    val availableFeaturesSet10 = getEnabledFeatures(availableFeatures10)
    val availableFeaturesSet11 = getEnabledFeatures(availableFeatures11)
    val availableFeaturesSet12 = getEnabledFeatures(availableFeatures12)

    val combinedFeatures10 = VkPhysicalDeviceFeatures.calloc(stack)
    val combinedFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
    combinedFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
    val combinedFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
    combinedFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)

    val requestedFeatures10 = VkPhysicalDeviceFeatures.calloc(stack)
    val requestedFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
    val requestedFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)

    val requiredFeatures10 = VkPhysicalDeviceFeatures.calloc(stack)
    val requiredFeatures11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
    val requiredFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)

    val deviceProperties = VkPhysicalDeviceProperties.calloc(stack)
    vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties)
    val deviceName = deviceProperties.deviceNameString()

    val extensionsToEnable = mutableSetOf<String>()
    val vrExtensions = vrManager.getVulkanDeviceExtensions(physicalDevice, deviceName, availableExtensions)
    if (!availableExtensions.containsAll(vrExtensions)) {
        throw ExtensionStartupException(
            "Missing required Vulkan device extensions",
            "The OpenVR runtime requires the following Vulkan device extensions to work, but not all of them are available.",
            availableExtensions, vrExtensions
        )
    }
    extensionsToEnable.addAll(vrExtensions)

    var nextChain: VkBaseOutStructure? = null

    val pluginActors = pluginManager.getImplementations(VulkanDeviceActor::class)
    for ((pluginActor, pluginInstance) in pluginActors) {

        requestedFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
        requiredFeatures11.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
        requestedFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
        requiredFeatures12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)

        val requestedExtensions = mutableSetOf<String>()
        val requiredExtensions = mutableSetOf<String>()

        val agent = VulkanDeviceActor.Agent(
            vkInstance, physicalDevice,
            availableExtensions, requestedExtensions, requiredExtensions,
            availableFeatures10, availableFeatures11, availableFeatures12,
            requestedFeatures10, requestedFeatures11, requestedFeatures12,
            requiredFeatures10, requiredFeatures11, requiredFeatures12,
            nextChain
        )

        pluginActor.manipulateVulkanDevice(pluginInstance, agent)

        val pluginName = pluginInstance.info.name
        logger.info("The $pluginName plug-in requested the following Vulkan device extensions:")
        for (extension in requestedExtensions) {
            logger.info(extension)
            if (availableExtensions.contains(extension)) {
                extensionsToEnable.add(extension)
            }
        }

        logger.info("The $pluginName plug-in requires the following Vulkan device extensions:")
        for (extension in requiredExtensions) {
            logger.info(extension)
        }

        if (!availableExtensions.containsAll(requiredExtensions)) {
            throw ExtensionStartupException(
                "Missing required Vulkan device extensions",
                "The ${pluginInstance.info.name} plug-in requires the following Vulkan device extensions to work, but not all of them are available.",
                availableExtensions, requiredExtensions
            )
        }
        extensionsToEnable.addAll(requiredExtensions)

        logger.info("The $pluginName plug-in requested the following Vulkan 1.0 device features:")
        for (feature in getEnabledFeatures(requestedFeatures10)) {
            logger.info(feature)
        }

        logger.info("The $pluginName plug-in requested the following Vulkan 1.1 device features:")
        for (feature in getEnabledFeatures(requestedFeatures11)) {
            logger.info(feature)
        }

        logger.info("The $pluginName plug-in requested the following Vulkan 1.2 device features:")
        for (feature in getEnabledFeatures(requestedFeatures12)) {
            logger.info(feature)
        }

        enableFeatures(combinedFeatures10, availableFeaturesSet10.intersect(getEnabledFeatures(requestedFeatures10)))
        enableFeatures(combinedFeatures11, availableFeaturesSet11.intersect(getEnabledFeatures(requestedFeatures11)))
        enableFeatures(combinedFeatures12, availableFeaturesSet12.intersect(getEnabledFeatures(requestedFeatures12)))

        fun missingFeatureException(version: String, availableFeatures: Set<String>, requiredFeatures: Set<String>) {
            throw ExtensionStartupException(
                "Missing required Vulkan device features",
                "The ${pluginInstance.info.name} plug-in requires the following Vulkan $version device features, but the chosen " +
                        "device $deviceName doesn't support them all.", availableFeatures, requiredFeatures, "features"
            )
        }

        val requiredFeaturesSet10 = getEnabledFeatures(requiredFeatures10)
        logger.info("The $pluginName plug-in requires the following Vulkan 1.0 device features:")
        for (feature in requiredFeaturesSet10) {
            logger.info(feature)
        }
        if (!availableFeaturesSet10.containsAll(requiredFeaturesSet10)) {
            missingFeatureException("1.0", availableFeaturesSet10, requiredFeaturesSet10)
        }
        enableFeatures(combinedFeatures10, requiredFeaturesSet10)

        val requiredFeaturesSet11 = getEnabledFeatures(requiredFeatures11)
        logger.info("The $pluginName plug-in requires the following Vulkan 1.1 device features:")
        for (feature in requiredFeaturesSet11) {
            logger.info(feature)
        }
        if (!availableFeaturesSet11.containsAll(requiredFeaturesSet11)) {
            missingFeatureException("1.1", availableFeaturesSet11, requiredFeaturesSet11)
        }
        enableFeatures(combinedFeatures11, requiredFeaturesSet11)

        val requiredFeaturesSet12 = getEnabledFeatures(requiredFeatures12)
        logger.info("The $pluginName plug-in requires the following Vulkan 1.2 device features:")
        for (feature in requiredFeaturesSet12) {
            logger.info(feature)
        }
        if (!availableFeaturesSet12.containsAll(requiredFeaturesSet12)) {
            missingFeatureException("1.2", availableFeaturesSet12, requiredFeaturesSet12)
        }
        enableFeatures(combinedFeatures12, requiredFeaturesSet12)

        // We need to clear this because it will be reused for the next plug-in
        requestedFeatures10.clear()
        requestedFeatures11.clear()
        requestedFeatures12.clear()
        requiredFeatures10.clear()
        requiredFeatures11.clear()
        requiredFeatures12.clear()

        nextChain = combineNextChains(nextChain, agent.extendNextChain)
    }

    // The physical device selection assures that a queue family with both graphics and compute support exists
    val generalQueueFamilyIndex = queueFamilies.withIndex().first { (_, queueFamily) ->
        (queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0 && (queueFamily.queueFlags() and VK_QUEUE_COMPUTE_BIT) != 0
    }.index

    // These two specialized queues are optional, but probably available
    val computeOnlyQueueFamilyIndex = queueFamilies.withIndex().firstOrNull { (queueFamilyIndex, queueFamily) ->
        (queueFamily.queueFlags() and VK_QUEUE_COMPUTE_BIT) != 0 && queueFamilyIndex != generalQueueFamilyIndex
    }?.index
    val transferOnlyQueueFamilyIndex = queueFamilies.withIndex().firstOrNull { (queueFamilyIndex, queueFamily) ->
        // Note that any queue family that supports graphics and/or compute operations implicitly supports transfer
        (queueFamily.queueFlags() and (VK_QUEUE_TRANSFER_BIT or VK_QUEUE_COMPUTE_BIT or VK_QUEUE_GRAPHICS_BIT)) != 0 &&
                queueFamilyIndex != generalQueueFamilyIndex && queueFamilyIndex != computeOnlyQueueFamilyIndex
    }?.index

    var numLogicalQueueFamilies = 1
    if (computeOnlyQueueFamilyIndex != null) {
        numLogicalQueueFamilies += 1
    }
    if (transferOnlyQueueFamilyIndex != null) {
        numLogicalQueueFamilies += 1
    }

    val generalPriorities = pickQueuePriorities(
        queueFamilies[generalQueueFamilyIndex].queueCount(), generalQueueFamilyIndex, stack
    )
    val computeOnlyPriorities = if (computeOnlyQueueFamilyIndex != null) {
        pickQueuePriorities(queueFamilies[computeOnlyQueueFamilyIndex].queueCount(), computeOnlyQueueFamilyIndex, stack)
    } else { null }
    val transferOnlyPriorities = if (transferOnlyQueueFamilyIndex != null) {
        pickQueuePriorities(queueFamilies[transferOnlyQueueFamilyIndex].queueCount(), transferOnlyQueueFamilyIndex, stack)
    } else { null }

    val cipQueues = VkDeviceQueueCreateInfo.calloc(numLogicalQueueFamilies, stack)
    val ciGeneralQueue = cipQueues[0]
    ciGeneralQueue.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
    ciGeneralQueue.queueFamilyIndex(generalQueueFamilyIndex)
    ciGeneralQueue.pQueuePriorities(generalPriorities.first)
    if (computeOnlyPriorities != null) {
        val ciComputeQueue = cipQueues[1]
        ciComputeQueue.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
        ciComputeQueue.queueFamilyIndex(computeOnlyPriorities.second.index)
        ciComputeQueue.pQueuePriorities(computeOnlyPriorities.first)
    }
    if (transferOnlyPriorities != null) {
        val ciTransferQueue = cipQueues[cipQueues.capacity() - 1]
        ciTransferQueue.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
        ciTransferQueue.queueFamilyIndex(transferOnlyPriorities.second.index)
        ciTransferQueue.pQueuePriorities(transferOnlyPriorities.first)
    }

    ciDevice.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
    ciDevice.pNext(combinedFeatures11.address())
    combinedFeatures11.pNext(combinedFeatures12.address())
    if (nextChain != null) {
        combinedFeatures12.pNext(nextChain.address())
    }
    ciDevice.ppEnabledExtensionNames(encodeStrings(extensionsToEnable, stack))
    ciDevice.pEnabledFeatures(combinedFeatures10)
    ciDevice.pQueueCreateInfos(cipQueues)

    return PopulateDeviceResult(
        extensionsToEnable, combinedFeatures10,
        combinedFeatures11, combinedFeatures12,
        generalPriorities.second, computeOnlyPriorities?.second, transferOnlyPriorities?.second
    )
}

private val featureFieldBlackList = listOf("SIZEOF", "ALIGNOF", "STYPE", "PNEXT")

private fun getEnabledFeatures(address: Long, featuresClass: Class<*>): Set<String> {
    return featuresClass.declaredFields.filter { field ->
        if (Modifier.isPublic(field.modifiers) && Modifier.isFinal(field.modifiers) &&
            field.type == Int::class.java && !featureFieldBlackList.contains(field.name)) {
            val fieldOffset = field.get(null) as Int
            memGetInt(address + fieldOffset) == 1
        } else {
            false
        }
    }.map { field -> field.name }.toSet()
}

fun getEnabledFeatures(features: VkPhysicalDeviceFeatures): Set<String> {
    return getEnabledFeatures(features.address(), features::class.java)
}

fun getEnabledFeatures(features: VkPhysicalDeviceVulkan11Features): Set<String> {
    return getEnabledFeatures(features.address(), features::class.java)
}

fun getEnabledFeatures(features: VkPhysicalDeviceVulkan12Features): Set<String> {
    return getEnabledFeatures(features.address(), features::class.java)
}

private fun enableFeatures(address: Long, featuresClass: Class<*>, featuresToEnable: Collection<String>) {
    featuresClass.declaredFields.forEach { field ->
        if (Modifier.isPublic(field.modifiers) && Modifier.isFinal(field.modifiers) &&
            field.type == Int::class.java && featuresToEnable.contains(field.name)) {
            val fieldOffset = field.get(null) as Int
            memPutInt(address + fieldOffset, 1)
        }
    }
}

fun enableFeatures(dest: VkPhysicalDeviceFeatures, featuresToEnable: Collection<String>) {
    enableFeatures(dest.address(), dest::class.java, featuresToEnable)
}

fun enableFeatures(dest: VkPhysicalDeviceVulkan11Features, featuresToEnable: Collection<String>) {
    enableFeatures(dest.address(), dest::class.java, featuresToEnable)
}

fun enableFeatures(dest: VkPhysicalDeviceVulkan12Features, featuresToEnable: Collection<String>) {
    enableFeatures(dest.address(), dest::class.java, featuresToEnable)
}

internal fun pickQueuePriorities(
    numAvailableQueues: Int, queueFamilyIndex: Int, stack: MemoryStack
): Pair<FloatBuffer, QueueFamilyInfo> {
    val numBackgroundQueues = numAvailableQueues / 2
    val numPriorityQueues = numAvailableQueues - numBackgroundQueues

    val result = stack.callocFloat(numPriorityQueues + numBackgroundQueues)
    repeat(numPriorityQueues) {
        result.put(1f)
    }
    repeat(numBackgroundQueues) {
        result.put(0f)
    }
    result.rewind()

    return Pair(result, QueueFamilyInfo(queueFamilyIndex, numPriorityQueues, numBackgroundQueues))
}

internal class PopulateDeviceResult(
    val enabledExtensions: Set<String>,
    val enabledFeatures10: VkPhysicalDeviceFeatures,
    val enabledFeatures11: VkPhysicalDeviceVulkan11Features,
    val enabledFeatures12: VkPhysicalDeviceVulkan12Features,
    val generalQueueFamily: QueueFamilyInfo,
    val computeOnlyQueueFamily: QueueFamilyInfo?,
    val transferOnlyQueueFamily: QueueFamilyInfo?
)

internal class QueueFamilyInfo(val index: Int, val numPriorityQueues: Int, val numBackgroundQueues: Int) {
    override fun equals(other: Any?): Boolean {
        return if (other is QueueFamilyInfo) {
            index == other.index && numPriorityQueues == other.numPriorityQueues && numBackgroundQueues == other.numBackgroundQueues
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + numPriorityQueues
        result = 31 * result + numBackgroundQueues
        return result
    }
}
