package dragons.vulkan.destroy

import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanInstanceDestructionListener
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VkInstance
import org.slf4j.LoggerFactory.getLogger

fun destroyVulkanInstance(vkInstance: VkInstance, pluginManager: PluginManager) {
    val logger = getLogger("Vulkan")

    val beforeAgent = VulkanInstanceDestructionListener.BeforeAgent(vkInstance)
    val listeners = pluginManager.getImplementations(VulkanInstanceDestructionListener::class)
    listeners.forEach { listenerPair -> listenerPair.first.beforeInstanceDestruction(listenerPair.second, beforeAgent) }

    logger.info("Destroying instance...")
    vkDestroyInstance(vkInstance, null)
    logger.info("Destroyed instance")

    val afterAgent = VulkanInstanceDestructionListener.AfterAgent()
    listeners.forEach { listenerPair -> listenerPair.first.afterInstanceDestruction(listenerPair.second, afterAgent)}
}
