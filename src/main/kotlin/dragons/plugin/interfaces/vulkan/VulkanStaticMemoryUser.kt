package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.vulkan.memory.claim.*
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface VulkanStaticMemoryUser: PluginInterface {
    fun claimStaticMemory(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val queueManager: QueueManager,
        val gameInitScope: CoroutineScope,
        val prefilledImages: MutableCollection<PrefilledImageMemoryClaim> = mutableListOf(),
        val uninitializedImages: MutableCollection<UninitializedImageMemoryClaim> = mutableListOf(),
        val prefilledBuffers: MutableCollection<PrefilledBufferMemoryClaim> = mutableListOf(),
        val uninitializedBuffers: MutableCollection<UninitializedBufferMemoryClaim> = mutableListOf(),
        val stagingBuffers: MutableCollection<StagingBufferMemoryClaim> = mutableListOf()
    )
}
