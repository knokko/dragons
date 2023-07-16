package dragons.plugins.debug.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanInstanceActor
import dragons.plugin.interfaces.vulkan.VulkanInstanceCreationListener
import dragons.plugin.interfaces.vulkan.VulkanInstanceDestructionListener
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.EXTValidationFeatures.*
import org.lwjgl.vulkan.VK12.VK_FALSE
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.vulkan.VkValidationFeaturesEXT
import org.slf4j.LoggerFactory.getLogger
import troll.exceptions.VulkanFailureException.assertVkSuccess

class DebugVulkanInstance: VulkanInstanceActor, VulkanInstanceCreationListener, VulkanInstanceDestructionListener {

    private var debugMessenger: Long? = null

    override fun manipulateVulkanInstanceExtensions(pluginInstance: PluginInstance, agent: VulkanInstanceActor.ExtensionAgent) {
        if (!pluginInstance.gameInitProps.mainParameters.forbidDebug) {
            agent.requiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
            agent.requiredExtensions.add(VK_EXT_VALIDATION_FEATURES_EXTENSION_NAME)
        }
    }

    override fun manipulateVulkanInstanceLayers(pluginInstance: PluginInstance, agent: VulkanInstanceActor.LayerAgent) {
        if (!pluginInstance.gameInitProps.mainParameters.forbidDebug) {
            agent.requiredLayers.add("VK_LAYER_KHRONOS_validation")
        }
    }

    override fun extendVulkanInstanceNextChain(pluginInstance: PluginInstance, agent: VulkanInstanceActor.NextChainAgent) {
        val validationFeatures = VkValidationFeaturesEXT.calloc(agent.stack)
        validationFeatures.`sType$Default`()
        validationFeatures.pEnabledValidationFeatures(agent.stack.ints(
                VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_EXT,
                VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_RESERVE_BINDING_SLOT_EXT,
                VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT,
                VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT
        ))
        validationFeatures.pDisabledValidationFeatures(null)

        agent.pNext = validationFeatures.address()
    }

    override fun afterVulkanInstanceCreation(
        pluginInstance: PluginInstance,
        agent: VulkanInstanceCreationListener.Agent
    ) {
        if (!pluginInstance.gameInitProps.mainParameters.forbidDebug) {
            stackPush().use { stack ->
                val logger = getLogger("Vulkan")

                val ciMessenger = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                ciMessenger.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                ciMessenger.messageSeverity(
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                ciMessenger.messageType(
                    VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                ciMessenger.pfnUserCallback { messageSeverity, messageTypes, pCallbackData, _ ->
                    val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)

                    var messageTypesString = ""
                    if ((messageTypes and VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
                        messageTypesString += "[Validation]"
                    }
                    if ((messageTypes and VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
                        messageTypesString += "[General]"
                    }
                    if ((messageTypes and VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
                        // This warning occurs every frame because SteamVR likes VK_IMAGE_LAYOUT_GENERAL
                        if (callbackData.messageIdNumber() == 1303270965) {
                            return@pfnUserCallback VK_FALSE
                        }
                        messageTypesString += "[Performance]"
                    }

                    val messageString =
                        "$messageTypesString ${callbackData.pMessageString()} (${callbackData.messageIdNumber()})"
                    if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) {
                        logger.info(messageString)
                    } else if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
                        logger.warn(messageString)
                    } else if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) {
                        logger.error(messageString)
                    } else {
                        logger.error("[UNKNOWN SEVERITY] $messageString")
                    }

                    VK_FALSE
                }

                val pMessenger = stack.callocLong(1)
                logger.info("Creating debug utils messenger...")
                assertVkSuccess(
                    vkCreateDebugUtilsMessengerEXT(agent.vulkanInstance, ciMessenger, null, pMessenger),
                    "CreateDebugUtilsMessengerEXT", null
                )
                logger.info("Created debug utils messenger")
                debugMessenger = pMessenger[0]
            }
        }
    }

    override fun beforeInstanceDestruction(
        pluginInstance: PluginInstance,
        agent: VulkanInstanceDestructionListener.BeforeAgent
    ) {
        if (!pluginInstance.gameInitProps.mainParameters.forbidDebug) {
            val logger = getLogger("Vulkan")

            logger.info("Destroying debug utils messenger...")
            vkDestroyDebugUtilsMessengerEXT(agent.vulkanInstance, debugMessenger!!, null)
            logger.info("Destroyed debug utils messenger")
        }
    }
}
