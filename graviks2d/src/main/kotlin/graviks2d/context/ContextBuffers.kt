package graviks2d.context

import graviks2d.pipeline.GraviksVertex
import graviks2d.pipeline.GraviksVertexBuffer
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.system.MemoryUtil.memIntBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*
import troll.buffer.MappedVmaBuffer
import java.nio.IntBuffer

internal class ContextBuffers(
    private val context: GraviksContext,
    /**
     * The maximum number of **vertices** that can fit in the vertex buffer
     */
    val vertexBufferSize: Int,
    /**
     * The maximum number of **int**s that can fit in the operation buffer
     */
    val operationBufferSize: Int
) {

    val vertexCpuBuffer: GraviksVertexBuffer
    val vertexBuffer: MappedVmaBuffer = context.instance.troll.buffers.createMapped(
        this.vertexBufferSize * GraviksVertex.BYTE_SIZE.toLong(),
        VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
        "GraviksVertexBuffer"
    )

    val operationCpuBuffer: IntBuffer
    val operationBuffer: MappedVmaBuffer

    init {
        if (vertexBuffer.buffer.size > Int.MAX_VALUE) throw IllegalArgumentException("Vertex buffer is too large")
        val rawCpuVertexBuffer = memByteBuffer(vertexBuffer.hostAddress, vertexBuffer.buffer.size.toInt())
        this.vertexCpuBuffer = GraviksVertexBuffer.createAtBuffer(rawCpuVertexBuffer, this.vertexBufferSize)

        this.operationBuffer = context.instance.troll.buffers.createMapped(
            this.operationBufferSize * 4L, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GraviksOperationBuffer"
        )
        if (operationBuffer.buffer.size > Int.MAX_VALUE) throw IllegalArgumentException("Operation buffer is too large")
        this.operationCpuBuffer = memIntBuffer(operationBuffer.hostAddress, operationBuffer.buffer.size.toInt())
    }

    fun destroy() {
        val troll = this.context.instance.troll
        vmaDestroyBuffer(troll.vmaAllocator(), this.operationBuffer.buffer.vkBuffer, this.operationBuffer.buffer.vmaAllocation)
        vmaDestroyBuffer(troll.vmaAllocator(), this.vertexBuffer.buffer.vkBuffer, this.vertexBuffer.buffer.vmaAllocation)
    }
}
