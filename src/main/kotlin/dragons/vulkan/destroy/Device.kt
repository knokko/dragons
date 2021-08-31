package dragons.vulkan.destroy

import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanDeviceDestructionListener
import org.lwjgl.vulkan.VK12.vkDestroyDevice
import org.lwjgl.vulkan.VK12.vkDeviceWaitIdle
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.slf4j.LoggerFactory.getLogger

fun destroyVulkanDevice(
    vkInstance: VkInstance, vkPhysicalDevice: VkPhysicalDevice, vkDevice: VkDevice,
    pluginManager: PluginManager
) {
    val beforeAgent = VulkanDeviceDestructionListener.BeforeAgent(
        vkInstance, vkPhysicalDevice, vkDevice
    )
    pluginManager.getImplementations(VulkanDeviceDestructionListener::class).forEach { pluginPair ->
        pluginPair.first.beforeDeviceDestruction(pluginPair.second, beforeAgent)
    }

    val logger = getLogger("Vulkan")

    logger.info("Waiting for logical device to become idle...")
    vkDeviceWaitIdle(vkDevice)
    logger.info("Destroying logical device...")
    vkDestroyDevice(vkDevice, null)
    logger.info("Destroyed logical device")

    val afterAgent = VulkanDeviceDestructionListener.AfterAgent(
        vkInstance, vkPhysicalDevice
    )
    pluginManager.getImplementations(VulkanDeviceDestructionListener::class).forEach { pluginPair ->
        pluginPair.first.afterDeviceDestruction(pluginPair.second, afterAgent)
    }
}
