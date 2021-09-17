package dragons.vulkan.init

import dragons.init.trouble.ExtensionStartupException
import dragons.init.trouble.SimpleStartupException
import dragons.init.trouble.StartupException
import dragons.init.trouble.VulkanStartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanInstanceActor
import dragons.plugin.interfaces.vulkan.VulkanInstanceCreationListener
import dragons.vr.VrManager
import dragons.vulkan.util.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import org.slf4j.LoggerFactory.getLogger
import java.nio.ByteBuffer
import kotlin.jvm.Throws

@Throws(StartupException::class)
fun initVulkanInstance(pluginManager: PluginManager, vrManager: VrManager): VkInstance {
    try {
        val logger = getLogger("Vulkan")
        val (layersToEnable, extensionsToEnable) = stackPush().use { stack ->

            val pNumAvailableExtensions = stack.callocInt(1)
            assertVkSuccess(
                vkEnumerateInstanceExtensionProperties(null as ByteBuffer?, pNumAvailableExtensions, null),
                "enumerateInstanceExtensionProperties", "count"
            )
            val numAvailableExtensions = pNumAvailableExtensions[0]

            val pAvailableExtensions = VkExtensionProperties.calloc(numAvailableExtensions, stack)
            assertVkSuccess(
                vkEnumerateInstanceExtensionProperties(
                    null as ByteBuffer?,
                    pNumAvailableExtensions,
                    pAvailableExtensions
                ),
                "EnumerateInstanceExtensionProperties", "extensions"
            )

            val availableExtensions = extensionBufferToSet(pAvailableExtensions)
            logger.info("There are ${availableExtensions.size} Vulkan instance extensions available:")
            for (extension in availableExtensions) {
                logger.info(extension)
            }

            val pNumAvailableLayers = stack.callocInt(1)
            assertVkSuccess(
                vkEnumerateInstanceLayerProperties(pNumAvailableLayers, null),
                "EnumerateInstanceLayerProperties", "count"
            )
            val numAvailableLayers = pNumAvailableLayers[0]
            val pAvailableLayers = VkLayerProperties.calloc(numAvailableLayers, stack)
            assertVkSuccess(
                vkEnumerateInstanceLayerProperties(pNumAvailableLayers, pAvailableLayers),
                "EnumerateInstanceLayerProperties", "layers"
            )
            val availableLayers = layerBufferToSet(pAvailableLayers)
            logger.info("There are ${availableLayers.size} available Vulkan instance layers:")
            for (layer in availableLayers) {
                logger.info(layer)
            }

            val vrExtensions = vrManager.getVulkanInstanceExtensions(availableExtensions)
            for (extension in vrExtensions) {
                if (!availableExtensions.contains(extension)) {
                    logger.error("The OpenVR runtime requires the $extension instance extension, which is not available")
                    throw ExtensionStartupException(
                        "Missing required Vulkan instance extensions",
                        "The OpenVR runtime requires the following Vulkan instance extensions to work, but not all of them are available.",
                        availableExtensions, vrExtensions
                    )
                }
            }

            val extensionsToEnable = mutableSetOf<String>()
            extensionsToEnable.addAll(vrExtensions)

            val layersToEnable = mutableSetOf<String>()

            val pluginPairs = pluginManager.getImplementations(VulkanInstanceActor::class)
            for (pluginPair in pluginPairs) {
                val pluginAgent = VulkanInstanceActor.Agent(
                    availableExtensions = availableExtensions,
                    desiredExtensions = mutableSetOf(),
                    requiredExtensions = mutableSetOf(),

                    availableLayers = availableLayers,
                    desiredLayers = mutableSetOf(),
                    requiredLayers = mutableSetOf()
                )
                pluginPair.first.manipulateVulkanInstance(pluginPair.second, pluginAgent)
                val pluginName = pluginPair.second.info.name

                if (!availableExtensions.containsAll(pluginAgent.requiredExtensions)) {
                    logger.error("Plug-in $pluginName requires the following instance extensions, but not all are available: ${pluginAgent.requiredExtensions}")
                    throw ExtensionStartupException(
                        "Missing required Vulkan instance extensions",
                        "The $pluginName plug-in requires the following Vulkan instance extensions to work, but not all of them are available.",
                        availableExtensions, pluginAgent.requiredExtensions
                    )
                }
                extensionsToEnable.addAll(pluginAgent.requiredExtensions)
                logger.info("Plug-in $pluginName requires the following instance extensions: ${pluginAgent.requiredExtensions}")
                for (extension in pluginAgent.desiredExtensions) {
                    if (availableExtensions.contains(extension)) {
                        extensionsToEnable.add(extension)
                    }
                }
                logger.info("Plug-in $pluginName requested the following instance extensions: ${pluginAgent.desiredExtensions}")

                if (!availableLayers.containsAll(pluginAgent.requiredLayers)) {
                    logger.error("Plug-in $pluginName requires the following layers, but not all are available: ${pluginAgent.requiredLayers}")
                    throw ExtensionStartupException(
                        "Missing required Vulkan layers",
                        "The $pluginName plug-in requires the following Vulkan layers to work, but not all of them are available.",
                        availableLayers, pluginAgent.requiredLayers, "layers"
                    )
                }
                layersToEnable.addAll(pluginAgent.requiredLayers)
                logger.info("Plug-in $pluginName requires the following layers: ${pluginAgent.requiredLayers}")
                for (layer in pluginAgent.desiredLayers) {
                    if (availableLayers.contains(layer)) {
                        layersToEnable.add(layer)
                    }
                }
                logger.info("Plug-in $pluginName requested the following layers: ${pluginAgent.desiredLayers}")
            }

            Pair(layersToEnable, extensionsToEnable)
        }

        logger.info("The following ${extensionsToEnable.size} instance extensions will be enabled:")
        for (extension in extensionsToEnable) {
            logger.info(extension)
        }
        logger.info("The following ${layersToEnable.size} layers will be enabled:")
        for (layer in layersToEnable) {
            logger.info(layer)
        }

        return stackPush().use { stack ->

            val appInfo = VkApplicationInfo.calloc(stack)
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            appInfo.pApplicationName(stack.UTF8("Dragons"))
            appInfo.applicationVersion(1)
            appInfo.apiVersion(VK_MAKE_VERSION(1, 2, 0))

            val ciInstance = VkInstanceCreateInfo.calloc(stack)
            ciInstance.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            ciInstance.pApplicationInfo(appInfo)
            ciInstance.ppEnabledExtensionNames(encodeStrings(extensionsToEnable, stack))
            ciInstance.ppEnabledLayerNames(encodeStrings(layersToEnable, stack))

            logger.info("Creating instance...")
            val pInstance = stack.callocPointer(1)
            val instanceCreationResult = vkCreateInstance(ciInstance, null, pInstance)

            if (instanceCreationResult == VK_ERROR_INCOMPATIBLE_DRIVER) {
                throw SimpleStartupException("Insufficient Vulkan support", listOf(
                    "This game requires at least Vulkan 1.2.0, but your graphics driver only supports Vulkan 1.0.",
                    "Updating your graphics drivers might resolve this problem."
                ))
            }
            assertVkSuccess(
                instanceCreationResult, "CreateInstance"
            )
            logger.info("Created instance")

            val pVulkanVersion = stack.callocInt(1)
            assertVkSuccess(
                vkEnumerateInstanceVersion(pVulkanVersion),
                "EnumerateInstanceVersion"
            )
            val vulkanVersion = pVulkanVersion[0]
            if (vulkanVersion < VK_MAKE_VERSION(1, 2, 0)) {
                val versionString = "${VK_VERSION_MAJOR(vulkanVersion)}.${VK_VERSION_MINOR(vulkanVersion)}.${VK_VERSION_PATCH(vulkanVersion)}"
                throw SimpleStartupException("Insufficient Vulkan support", listOf(
                    "This game requires at least Vulkan 1.2.0, but your graphics driver only supports up to Vulkan $versionString.",
                    "Updating your graphics drivers might resolve this problem"
                ))
            }

            val vkInstance = VkInstance(pInstance[0], ciInstance)

            val creationAgent = VulkanInstanceCreationListener.Agent(
                vulkanInstance = vkInstance,
                enabledLayers = layersToEnable,
                enabledExtensions = extensionsToEnable,
                ciInstance = ciInstance
            )
            pluginManager.getImplementations(VulkanInstanceCreationListener::class).forEach { listenerPair ->
                listenerPair.first.afterVulkanInstanceCreation(listenerPair.second, creationAgent)
            }

            vkInstance
        }
    } catch(failure: VulkanFailureException) {
        throw VulkanStartupException(failure)
    }
}
