package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class BufferMemoryClaim(
    val size: Int, val usageFlags: Int, val dstAccessMask: Int, val queueFamily: QueueFamily?,
    val storeResult: CompletableDeferred<VulkanBufferRange>, val prefill: ((ByteBuffer) -> Unit)?
) {
    init {
        if (size <= 0) throw IllegalArgumentException("Size ($size) must be positive")
    }
}

class StagingBufferMemoryClaim(
    val size: Int, val queueFamily: QueueFamily?, val storeResult: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>
) {
    init {
        if (size <= 0) throw IllegalArgumentException("Size ($size) must be positive")
    }
}
