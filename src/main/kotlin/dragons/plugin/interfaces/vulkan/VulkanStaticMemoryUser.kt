package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.vulkan.memory.claim.*
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceLimits

interface VulkanStaticMemoryUser: PluginInterface {
    fun claimStaticMemory(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val vkPhysicalDevice: VkPhysicalDevice,
        /**
         * Note: Only use this *during* the call to `claimStaticMemory`: do **not** store it! This is expected to be
         * short-lived.
         */
        val vkPhysicalDeviceLimits: VkPhysicalDeviceLimits,
        val vkDevice: VkDevice,
        val queueManager: QueueManager,
        val gameInitScope: CoroutineScope,
        val claims: MemoryScopeClaims,
        val pluginClassLoader: ClassLoader
    )
}
