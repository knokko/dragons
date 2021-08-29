package dragons.vulkan.destroy

import dragons.plugin.PluginManager
import org.lwjgl.vulkan.VK12.vkDestroyDevice
import org.lwjgl.vulkan.VK12.vkDeviceWaitIdle
import org.lwjgl.vulkan.VkDevice

fun destroyVulkanDevice(vkDevice: VkDevice, pluginManager: PluginManager) {
    vkDeviceWaitIdle(vkDevice)
    vkDestroyDevice(vkDevice, null)
}
