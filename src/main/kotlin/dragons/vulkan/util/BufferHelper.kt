package dragons.vulkan.util

import org.lwjgl.system.MemoryStack
import java.nio.LongBuffer

fun collectionToBuffer(collection: Collection<Long>, stack: MemoryStack): LongBuffer {
    val buffer = stack.callocLong(collection.size)
    for ((index, value) in collection.withIndex()) {
        buffer.put(index, value)
    }
    return buffer
}
