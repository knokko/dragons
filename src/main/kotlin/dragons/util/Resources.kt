package dragons.util

import org.lwjgl.system.MemoryUtil.memAlloc
import java.nio.ByteBuffer

/**
 * Loads the resource with the given *path* from the given *classLoader*. If the resource is found, a direct
 * *ByteBuffer* is allocated using *memAlloc*, filled with all bytes from the resource, and returned. If the resource is
 * not found, *null* is returned instead.
 *
 * **Note: if the result is not null, it should be freed using *memFree*!**
 */
fun mallocBundledResource(path: String, classLoader: ClassLoader): ByteBuffer? {
    val inputStream = classLoader.getResourceAsStream(path) ?: return null
    val byteArray = inputStream.readAllBytes()
    val byteBuffer = memAlloc(byteArray.size)
    byteBuffer.put(0, byteArray)
    return byteBuffer
}
