package graviks2d.pipeline

import org.lwjgl.system.MemoryUtil.*
import java.nio.ByteBuffer

private fun writeOnly(): Nothing = throw UnsupportedOperationException("GraviksVertex is write-only for performance reasons")

@JvmInline
internal value class GraviksVertex(val address: Long) {

    var x: Float
    get() = writeOnly()
    set(value) = memPutFloat(address + OFFSET_X, value)

    var y: Float
    get() = writeOnly()
    set(value) = memPutFloat(address + OFFSET_Y, value)

    var depth: Int
    get() = writeOnly()
    set(value) = memPutInt(address + OFFSET_DEPTH, value)

    var operationIndex: Int
    get() = writeOnly()
    set(value) = memPutInt(address + OFFSET_OPERATION_INDEX, value)

    companion object {
        const val OFFSET_X = 0
        const val OFFSET_Y = OFFSET_X + 4
        const val OFFSET_DEPTH = OFFSET_Y + 4
        const val OFFSET_OPERATION_INDEX = OFFSET_DEPTH + 4

        const val BYTE_SIZE = OFFSET_OPERATION_INDEX + 4
    }
}

internal class GraviksVertexBuffer private constructor(
    val baseAddress: Long,
    val numVertices: Int
) {
    operator fun get(index: Int): GraviksVertex {
        if (index < 0) throw IllegalArgumentException("Index ($index) can't be negative")
        if (index >= numVertices) throw IllegalArgumentException("Index ($index) must be smaller than $numVertices")
        return GraviksVertex(baseAddress + GraviksVertex.BYTE_SIZE * index)
    }

    companion object {
        fun createAtBuffer(buffer: ByteBuffer, numVertices: Int): GraviksVertexBuffer {
            val numBytes = GraviksVertex.BYTE_SIZE * numVertices
            if (GraviksVertex.BYTE_SIZE * numVertices > buffer.remaining()) {
                throw IllegalArgumentException("Buffer has ${buffer.remaining()} remaining bytes, but $numBytes are needed")
            }

            return GraviksVertexBuffer(memAddress(buffer), numVertices)
        }
    }
}
