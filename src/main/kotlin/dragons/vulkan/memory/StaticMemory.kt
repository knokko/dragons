package dragons.vulkan.memory

import dragons.init.trouble.SimpleStartupException
import dragons.plugin.interfaces.vulkan.VulkanStaticMemoryUser
import dragons.vr.VrManager
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.scope.MemoryScope
import dragons.vulkan.memory.scope.MemoryScopeClaims
import dragons.vulkan.memory.scope.packMemoryClaims
import dragons.vulkan.queue.QueueManager
import knokko.plugin.PluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.vkGetPhysicalDeviceProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

private suspend fun getFinishedStaticMemoryAgents(
        logger: Logger, pluginManager: PluginManager, pluginClassLoader: ClassLoader, vrManager: VrManager,
        queueManager: QueueManager, renderImageInfo: RenderImageInfo,
        vkPhysicalDevice: VkPhysicalDevice, vkPhysicalDeviceLimits: VkPhysicalDeviceLimits, vkDevice: VkDevice,
        scope: CoroutineScope
): Pair<Collection<MemoryScopeClaims>, CoreStaticMemoryPending> {

    logger.info("Calling all VulkanStaticMemoryUsers...")
    val memoryUsers = pluginManager.getImplementations(VulkanStaticMemoryUser::class)
    val pluginTasks = memoryUsers.map { (memoryUser, pluginInstance) ->
        scope.async {
            val agent = VulkanStaticMemoryUser.Agent(
                vkPhysicalDevice, vkPhysicalDeviceLimits, vkDevice,
                queueManager, scope, MemoryScopeClaims(), pluginClassLoader
            )
            memoryUser.claimStaticMemory(pluginInstance, agent)
            agent
        }
    }
    logger.info("All calls to the VulkanStaticMemoryUsers started")
    val finishedClaims = pluginTasks.map { task -> task.await().claims }
    logger.info("All calls to the VulkanStaticMemoryUsers finished")

    // The game core also needs to add some static resources...
    val customAgent = VulkanStaticMemoryUser.Agent(
        vkPhysicalDevice, vkPhysicalDeviceLimits, vkDevice,
        queueManager, scope, MemoryScopeClaims(), pluginClassLoader
    )
    val pendingCoreMemory = claimStaticCoreMemory(customAgent, vrManager, queueManager, renderImageInfo)

    // And the VrManager may also need some static resources...
    val vrAgent = VulkanStaticMemoryUser.Agent(
        vkPhysicalDevice, vkPhysicalDeviceLimits, vkDevice,
        queueManager, scope, MemoryScopeClaims(), pluginClassLoader
    )
    vrManager.claimStaticMemory(vrAgent, queueManager, renderImageInfo)

    return Pair(finishedClaims + listOf(customAgent.claims, vrAgent.claims), pendingCoreMemory)
}

@Throws(SimpleStartupException::class)
suspend fun allocateStaticMemory(
    vkPhysicalDevice: VkPhysicalDevice, vkDevice: VkDevice, queueManager: QueueManager,
    pluginManager: PluginManager, pluginClassLoader: ClassLoader,
    vrManager: VrManager, memoryInfo: MemoryInfo, renderImageInfo: RenderImageInfo, scope: CoroutineScope
): Pair<MemoryScope, CoreStaticMemory> {
    val logger = getLogger("Vulkan")

    val (finishedAgents, pendingCoreMemory) = stackPush().use { stack ->
        val vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.calloc(stack)
        vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties)

        getFinishedStaticMemoryAgents(
            logger, pluginManager, pluginClassLoader, vrManager, queueManager, renderImageInfo,
            vkPhysicalDevice, vkPhysicalDeviceProperties.limits(), vkDevice, scope
        )
    }

    val staticMemoryScope = packMemoryClaims(vkDevice, queueManager, memoryInfo, scope, finishedAgents, "static")
    val coreStaticMemory = pendingCoreMemory.awaitCompletely()

    return Pair(staticMemoryScope, coreStaticMemory)
}
