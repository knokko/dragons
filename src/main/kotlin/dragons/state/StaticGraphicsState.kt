package dragons.state

import dragons.vr.ResolveHelper
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.CoreStaticMemory
import dragons.vulkan.memory.scope.MemoryScope
import dragons.vulkan.queue.QueueManager
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice

class StaticGraphicsState(
    val vkInstance: VkInstance,
    val vkPhysicalDevice: VkPhysicalDevice,
    val vkDevice: VkDevice,
    val queueManager: QueueManager,

    val renderImageInfo: RenderImageInfo,

    val memoryScope: MemoryScope,
    val coreMemory: CoreStaticMemory,
)
