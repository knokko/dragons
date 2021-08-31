package dragons.vulkan.memory.claim

import java.nio.ByteBuffer
import java.util.function.Consumer

class PrefilledBufferMemoryClaim(val size: Int, val usageFlags: Int, val prefill: Consumer<ByteBuffer>)

class UninitializedBufferMemoryClaim(val size: Int, val usageFlags: Int)
