package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.vulkan.memory.claim.*
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface VulkanStaticMemoryUser: PluginInterface {
    fun claimStaticMemory(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val queueManager: QueueManager,
        val gameInitScope: CoroutineScope,
        val claims: MemoryScopeClaims,
        val pluginClassLoader: ClassLoader
    )
}
