package dragons.state

import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.CoreStaticMemory
import dragons.vulkan.memory.MemoryInfo
import dragons.vulkan.memory.scope.MemoryScope
import dragons.vulkan.queue.QueueManager
import graviks2d.core.GraviksInstance
import troll.instance.TrollInstance

class StaticGraphicsState(
    val troll: TrollInstance,
    val queueManager: QueueManager,
    val memoryInfo: MemoryInfo,

    val renderImageInfo: RenderImageInfo,

    val memoryScope: MemoryScope,
    val coreMemory: CoreStaticMemory,
    val graviksInstance: GraviksInstance
)
