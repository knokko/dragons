package dragons.vulkan.memory.claim

import dragons.vulkan.queue.QueueFamily
import java.nio.ByteBuffer

class PrefilledBufferMemoryClaim(
    val size: Int, val usageFlags: Int, val queueFamily: QueueFamily?, val prefill: (ByteBuffer) -> Unit
)

class UninitializedBufferMemoryClaim(val size: Int, val usageFlags: Int, val queueFamily: QueueFamily?)
