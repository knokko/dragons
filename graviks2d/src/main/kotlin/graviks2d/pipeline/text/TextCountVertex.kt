package graviks2d.pipeline.text

import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

private fun writeOnly(): Nothing = throw UnsupportedOperationException("TextVertex is write-only for performance reasons")

internal const val OPERATION_INCREMENT = 1
internal const val OPERATION_CORRECT_START = 2
internal const val OPERATION_CORRECT_CONTROL = 3
internal const val OPERATION_CORRECT_END = 4

@JvmInline
internal value class TextCountVertex(val address: Long) {

    var x: Float
        get() = writeOnly()
        set(value) = MemoryUtil.memPutFloat(address + OFFSET_X, value)

    var y: Float
        get() = writeOnly()
        set(value) = MemoryUtil.memPutFloat(address + OFFSET_Y, value)

    var operation: Int
        get() = writeOnly()
        set(value) = MemoryUtil.memPutInt(address + OFFSET_OPERATION, value)

    companion object {
        const val OFFSET_X = 0
        const val OFFSET_Y = OFFSET_X + 4
        const val OFFSET_OPERATION = OFFSET_Y + 4

        const val BYTE_SIZE = OFFSET_OPERATION + 4
    }
}

internal class TextVertexBuffer private constructor(
    val baseAddress: Long,
    val numVertices: Int
) {
    operator fun get(index: Int): TextCountVertex {
        if (index < 0) throw IllegalArgumentException("Index ($index) can't be negative")
        if (index >= numVertices) throw IllegalArgumentException("Index ($index) must be smaller than $numVertices")
        return TextCountVertex(baseAddress + TextCountVertex.BYTE_SIZE * index)
    }

    companion object {
        fun createAtBuffer(buffer: ByteBuffer, numVertices: Int): TextVertexBuffer {
            val numBytes = TextCountVertex.BYTE_SIZE * numVertices
            if (TextCountVertex.BYTE_SIZE * numVertices > buffer.remaining()) {
                throw IllegalArgumentException("Buffer has ${buffer.remaining()} remaining bytes, but $numBytes are needed")
            }

            return TextVertexBuffer(MemoryUtil.memAddress(buffer), numVertices)
        }
    }
}
