package dragons.state

import com.github.knokko.boiler.instance.BoilerInstance
import dragons.vulkan.RenderImageInfo
import dragons.vulkan.memory.CoreStaticMemory
import dragons.vulkan.memory.MemoryInfo
import dragons.vulkan.memory.scope.MemoryScope
import dragons.vulkan.queue.QueueManager
import graviks2d.core.GraviksInstance

class StaticGraphicsState(
    val boiler: BoilerInstance,
    val queueManager: QueueManager,
    val memoryInfo: MemoryInfo,

    val renderImageInfo: RenderImageInfo,

    val memoryScope: MemoryScope,
    val coreMemory: CoreStaticMemory,
    val graviksInstance: GraviksInstance
)
