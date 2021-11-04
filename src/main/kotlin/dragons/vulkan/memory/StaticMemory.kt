package dragons.vulkan.memory

import dragons.init.trouble.SimpleStartupException
import dragons.plugin.PluginManager
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vr.VrManager
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.scope.MemoryScope
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.memory.scope.packMemoryClaims
import dragons.vulkan.queue.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.lwjgl.vulkan.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

private suspend fun getFinishedStaticMemoryAgents(
    logger: Logger, pluginManager: PluginManager, vrManager: VrManager, queueManager: QueueManager,
    renderImageInfo: RenderImageInfo, scope: CoroutineScope
): Pair<Collection<MemoryScopeClaims>, CoreStaticMemoryPending> {

    logger.info("Calling all VulkanStaticMemoryUsers...")
    val memoryUsers = pluginManager.getImplementations(VulkanStaticMemoryUser::class)
    val pluginTasks = memoryUsers.map { (memoryUser, pluginInstance) ->
        scope.async {
            val agent = VulkanStaticMemoryUser.Agent(queueManager, scope, MemoryScopeClaims())
            memoryUser.claimStaticMemory(pluginInstance, agent)
            agent
        }
    }
    logger.info("All calls to the VulkanStaticMemoryUsers started")
    val finishedClaims = pluginTasks.map { task -> task.await().claims }
    logger.info("All calls to the VulkanStaticMemoryUsers finished")

    // The game core also needs to add some static resources...
    val customAgent = VulkanStaticMemoryUser.Agent(queueManager, scope, MemoryScopeClaims())
    val pendingCoreMemory = claimStaticCoreMemory(customAgent, vrManager, queueManager, renderImageInfo)

    return Pair(finishedClaims + listOf(customAgent.claims), pendingCoreMemory)
}

@Throws(SimpleStartupException::class)
suspend fun allocateStaticMemory(
    vkPhysicalDevice: VkPhysicalDevice, vkDevice: VkDevice,
    queueManager: QueueManager, pluginManager: PluginManager, vrManager: VrManager,
    memoryInfo: MemoryInfo, renderImageInfo: RenderImageInfo, scope: CoroutineScope
): Pair<MemoryScope, CoreStaticMemory> {
    val logger = getLogger("Vulkan")

    val (finishedAgents, pendingCoreMemory) = getFinishedStaticMemoryAgents(
        logger, pluginManager, vrManager, queueManager, renderImageInfo, scope
    )

    val staticMemoryScope = packMemoryClaims(vkDevice, queueManager, memoryInfo, scope, finishedAgents, "static")
    val coreStaticMemory = pendingCoreMemory.awaitCompletely()

    return Pair(staticMemoryScope, coreStaticMemory)
}
