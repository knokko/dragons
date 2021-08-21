package dragons.vr

import org.lwjgl.openvr.VR
import org.lwjgl.openvr.VRCompositor.VRCompositor_GetVulkanDeviceExtensionsRequired
import org.lwjgl.openvr.VRCompositor.VRCompositor_GetVulkanInstanceExtensionsRequired
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VkPhysicalDevice
import org.slf4j.LoggerFactory.getLogger

class OpenVrManager: VrManager {
    override fun getVulkanInstanceExtensions(availableExtensions: Set<String>): Set<String> {
        val extensionStringSize = VRCompositor_GetVulkanInstanceExtensionsRequired(null)
        val logger = getLogger("VR")
        val result = mutableSetOf<String>()
        if (extensionStringSize > 0) {
            val extensionString = stackPush().use { stack ->
                val extensionBuffer = stack.calloc(extensionStringSize)
                VRCompositor_GetVulkanInstanceExtensionsRequired(extensionBuffer)
                memUTF8(memAddress(extensionBuffer))
            }

            val extensionArray = extensionString.split(" ")
            logger.info("The following ${extensionArray.size} Vulkan instance extensions are required for OpenVR:")
            for (extension in extensionArray) {
                logger.info(extension)
            }

            result.addAll(extensionArray)
        }

        return result
    }

    override fun getVulkanDeviceExtensions(device: VkPhysicalDevice, deviceName: String, availableExtensions: Set<String>): Set<String> {
        val extensionStringSize = VRCompositor_GetVulkanDeviceExtensionsRequired(device.address(), null)
        val logger = getLogger("VR")
        val result = mutableSetOf<String>()
        if (extensionStringSize > 0) {
            val extensionString = stackPush().use { stack ->
                val extensionBuffer = stack.calloc(extensionStringSize)
                VRCompositor_GetVulkanDeviceExtensionsRequired(device.address(), extensionBuffer)
                memUTF8(memAddress(extensionBuffer))
            }

            val extensionArray = extensionString.split(" ")
            logger.info("The following ${extensionArray.size} Vulkan device extensions are required for $deviceName in OpenVR:")
            for (extension in extensionArray) {
                logger.info(extension)
            }

            result.addAll(extensionArray)
        }

        return result
    }

    override fun destroy() {
        getLogger("VR").info("Shutting down OpenVR")
        VR.VR_ShutdownInternal()
    }
}
