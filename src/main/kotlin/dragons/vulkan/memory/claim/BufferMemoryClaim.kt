package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class BufferMemoryClaim(
    val size: Int, val usageFlags: Int,
    val dstAccessMask: Int = 0, val dstPipelineStageMask: Int = 0, val alignment: Int = 1, val queueFamily: QueueFamily?,
    val storeResult: CompletableDeferred<VulkanBufferRange>, val prefill: ((ByteBuffer) -> Unit)?
) {
    init {
        if (size <= 0) throw IllegalArgumentException("Size ($size) must be positive")
        if (prefill != null && dstAccessMask == 0) throw IllegalArgumentException("You need to state dstAccessMask")
        if (prefill != null && dstPipelineStageMask == 0) throw IllegalArgumentException("You need to set dstPipelineStageMask")
    }

    override fun toString() = "BufferMemoryClaim(size = $size, queueFamily = $queueFamily)"
}

class StagingBufferMemoryClaim(
    val size: Int, val queueFamily: QueueFamily?, val usageFlags: Int = 0,
    val storeResult: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>
) {
    init {
        if (size <= 0) throw IllegalArgumentException("Size ($size) must be positive")
    }

    override fun toString() = "StagingBufferMemoryClaim(size = $size, queueFamily = $queueFamily)"
}
