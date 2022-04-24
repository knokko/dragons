package dragons.state

import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.CoreStaticMemory
import dragons.vulkan.memory.scope.MemoryScope
import dragons.vulkan.queue.QueueManager
import graviks2d.core.GraviksInstance
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice

class StaticGraphicsState(
    val vkInstance: VkInstance,
    val vkPhysicalDevice: VkPhysicalDevice,
    val vkDevice: VkDevice,
    val queueManager: QueueManager,
    val vmaAllocator: Long,

    val renderImageInfo: RenderImageInfo,

    val memoryScope: MemoryScope,
    val coreMemory: CoreStaticMemory,
    val graviksInstance: GraviksInstance
)
