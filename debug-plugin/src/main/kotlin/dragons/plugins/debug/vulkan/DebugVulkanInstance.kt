package dragons.plugins.debug.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.vulkan.VulkanInstanceActor
import dragons.plugin.interfaces.vulkan.VulkanInstanceCreationListener
import dragons.plugin.interfaces.vulkan.VulkanInstanceDestructionListener
import dragons.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.slf4j.LoggerFactory.getLogger

class DebugVulkanInstance: VulkanInstanceActor, VulkanInstanceCreationListener, VulkanInstanceDestructionListener {

    private var debugMessenger: Long? = null

    override fun manipulateVulkanInstance(pluginInstance: PluginInstance, agent: VulkanInstanceActor.Agent) {
        if (!pluginInstance.gameInitProps.mainParameters.forbidDebug) {
            agent.requiredLayers.add("VK_LAYER_KHRONOS_validation")
            agent.requiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
        }
    }

    override fun afterVulkanInstanceCreation(
        pluginInstance: PluginInstance,
        agent: VulkanInstanceCreationListener.Agent
    ) {
        stackPush().use { stack ->
            val logger = getLogger("Vulkan")

            val ciMessenger = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack)
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
                    messageTypesString += "[Performance]"
                }

                val messageString = "$messageTypesString ${callbackData.pMessageString()} (${callbackData.messageIdNumber()})"
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
                "CreateDebugUtilsMessengerEXT"
            )
            logger.info("Created debug utils messenger")
            debugMessenger = pMessenger[0]
        }
    }

    override fun beforeInstanceDestruction(
        pluginInstance: PluginInstance,
        agent: VulkanInstanceDestructionListener.BeforeAgent
    ) {
        val logger = getLogger("Vulkan")

        logger.info("Destroying debug utils messenger...")
        vkDestroyDebugUtilsMessengerEXT(agent.vulkanInstance, debugMessenger!!, null)
        logger.info("Destroyed debug utils messenger")
    }
}
