package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanBufferRange
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class PrefilledBufferMemoryClaim(
    val size: Int, val usageFlags: Int, val queueFamily: QueueFamily?,
    val storeResult: CompletableDeferred<VulkanBufferRange>, val prefill: (ByteBuffer) -> Unit
)

class UninitializedBufferMemoryClaim(
    val size: Int, val usageFlags: Int, val queueFamily: QueueFamily?,
    val storeResult: CompletableDeferred<VulkanBufferRange>)

class StagingBufferMemoryClaim(
    val size: Int, val queueFamily: QueueFamily?, val storeResult: CompletableDeferred<Pair<ByteBuffer, VulkanBufferRange>>
)
