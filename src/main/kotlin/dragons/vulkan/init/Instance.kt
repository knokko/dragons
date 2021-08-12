package dragons.vulkan.init

import dragons.init.trouble.ExtensionStartupException
import dragons.init.trouble.StartupException
import dragons.init.trouble.VulkanStartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanInstanceActor
import dragons.util.VulkanFailureException
import dragons.util.assertVkSuccess
import dragons.vr.VrManager
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

            val pAvailableExtensions = VkExtensionProperties.callocStack(numAvailableExtensions, stack)
            assertVkSuccess(
                vkEnumerateInstanceExtensionProperties(
                    null as ByteBuffer?,
                    pNumAvailableExtensions,
                    pAvailableExtensions
                ),
                "EnumerateInstanceExtensionProperties", "extensions"
            )

            val availableExtensions = HashSet<String>()
            for (extensionIndex in 0 until numAvailableExtensions) {
                availableExtensions.add(pAvailableExtensions[extensionIndex].extensionNameString())
            }
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
            val pAvailableLayers = VkLayerProperties.callocStack(numAvailableLayers, stack)
            assertVkSuccess(
                vkEnumerateInstanceLayerProperties(pNumAvailableLayers, pAvailableLayers),
                "EnumerateInstanceLayerProperties", "layers"
            )
            val availableLayers = HashSet<String>()
            for (index in 0 until numAvailableLayers) {
                availableLayers.add(pAvailableLayers[index].layerNameString())
            }
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
                pluginPair.first.manipulateVulkanInstance(pluginAgent)
                val pluginName = pluginPair.second.name

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

            val pChosenExtensions = stack.callocPointer(extensionsToEnable.size)
            for ((index, extension) in extensionsToEnable.withIndex()) {
                pChosenExtensions.put(index, stack.UTF8(extension))
            }

            val pChosenLayers = stack.callocPointer(layersToEnable.size)
            for ((index, layer) in layersToEnable.withIndex()) {
                pChosenLayers.put(index, stack.UTF8(layer))
            }

            val appInfo = VkApplicationInfo.callocStack(stack)
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            appInfo.pApplicationName(stack.UTF8("Dragons"))
            appInfo.applicationVersion(1)
            appInfo.apiVersion(VK_MAKE_VERSION(1, 2, 0))

            val ciInstance = VkInstanceCreateInfo.callocStack(stack)
            ciInstance.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            ciInstance.pApplicationInfo(appInfo)
            ciInstance.ppEnabledExtensionNames(pChosenExtensions)
            ciInstance.ppEnabledLayerNames(pChosenLayers)

            val pInstance = stack.callocPointer(1)
            assertVkSuccess(
                vkCreateInstance(ciInstance, null, pInstance),
                "CreateInstance"
            )

            VkInstance(pInstance[0], ciInstance)
        }
    } catch(failure: VulkanFailureException) {
        throw VulkanStartupException(failure)
    }
}
