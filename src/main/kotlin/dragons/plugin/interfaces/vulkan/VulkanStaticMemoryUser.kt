package dragons.plugin.interfaces.vulkan

import dragons.plugin.PluginInstance
import dragons.plugin.interfaces.PluginInterface
import dragons.vulkan.memory.claim.PrefilledBufferMemoryClaim
import dragons.vulkan.memory.claim.PrefilledImageMemoryClaim
import dragons.vulkan.memory.claim.UninitializedBufferMemoryClaim
import dragons.vulkan.memory.claim.UninitializedImageMemoryClaim
import dragons.vulkan.queue.QueueManager

interface VulkanStaticMemoryUser: PluginInterface {
    fun claimStaticMemory(pluginInstance: PluginInstance, agent: Agent)

    class Agent(
        val queueManager: QueueManager,
        val prefilledImages: MutableCollection<PrefilledImageMemoryClaim> = mutableListOf(),
        val uninitializedImages: MutableCollection<UninitializedImageMemoryClaim> = mutableListOf(),
        val prefilledBuffers: MutableCollection<PrefilledBufferMemoryClaim> = mutableListOf(),
        val uninitializedBuffers: MutableCollection<UninitializedBufferMemoryClaim> = mutableListOf()
    )
}
