package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class BufferMemoryClaim(
    val size: Int, val usageFlags: Int, val dstAccessMask: Int = 0, val queueFamily: QueueFamily?,
    val storeResult: CompletableDeferred<VulkanBufferRange>, val prefill: ((ByteBuffer) -> Unit)?
) {
    init {
        if (size <= 0) throw IllegalArgumentException("Size ($size) must be positive")
        if (prefill != null && dstAccessMask == 0) throw IllegalArgumentException("You need to state dstAccessMask")
    }
}

class StagingBufferMemoryClaim(
    val size: Int, val queueFamily: QueueFamily?, val storeResult: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>
) {
    init {
        if (size <= 0) throw IllegalArgumentException("Size ($size) must be positive")
    }
}
