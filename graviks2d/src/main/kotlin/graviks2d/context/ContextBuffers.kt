package graviks2d.context

import graviks2d.pipeline.GraviksVertex
import graviks2d.pipeline.GraviksVertexBuffer
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memIntBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
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
    val vertexVkBuffer: Long
    val vertexBufferAllocation: Long

    val operationCpuBuffer: IntBuffer
    val operationVkBuffer: Long
    val operationBufferAllocation: Long

    init {
        stackPush().use { stack ->

            val ciVertexBuffer = VkBufferCreateInfo.calloc(stack)
            ciVertexBuffer.`sType$Default`()
            ciVertexBuffer.size(this.vertexBufferSize * GraviksVertex.BYTE_SIZE.toLong())
            if (ciVertexBuffer.size() > Int.MAX_VALUE) {
                throw IllegalArgumentException("Too many vertices (${this.vertexBufferSize})")
            }
            ciVertexBuffer.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
            ciVertexBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val ciVertexAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciVertexAllocation.flags(
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or
                        VMA_ALLOCATION_CREATE_MAPPED_BIT
            )
            ciVertexAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val vertexAllocationInfo = VmaAllocationInfo.calloc(stack)

            val pVertexBuffer = stack.callocLong(1)
            val pVertexAllocation = stack.callocPointer(1)

            assertSuccess(
                vmaCreateBuffer(
                    this.context.instance.troll.vmaAllocator(),
                    ciVertexBuffer, ciVertexAllocation, pVertexBuffer, pVertexAllocation, vertexAllocationInfo
                ), "vmaCreateBuffer"
            )
            this.vertexVkBuffer = pVertexBuffer[0]
            this.vertexBufferAllocation = pVertexAllocation[0]

            val vertexBufferCpuAddress = vertexAllocationInfo.pMappedData()
            val rawCpuVertexBuffer = MemoryUtil.memByteBuffer(vertexBufferCpuAddress, ciVertexBuffer.size().toInt())

            this.vertexCpuBuffer = GraviksVertexBuffer.createAtBuffer(rawCpuVertexBuffer, this.vertexBufferSize)

            val ciOperationBuffer = VkBufferCreateInfo.calloc(stack)
            ciOperationBuffer.`sType$Default`()
            ciOperationBuffer.size(this.operationBufferSize * 4L)
            ciOperationBuffer.usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT)
            ciOperationBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            if (ciOperationBuffer.size() > Int.MAX_VALUE) {
                throw IllegalArgumentException("Operation buffer is too big ($operationBufferSize)")
            }

            val ciOperationAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciOperationAllocation.flags(
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or
                        VMA_ALLOCATION_CREATE_MAPPED_BIT
            )
            ciOperationAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val operationAllocationInfo = VmaAllocationInfo.calloc(stack)

            val pOperationBuffer = stack.callocLong(1)
            val pOperationAllocation = stack.callocPointer(1)
            assertSuccess(
                vmaCreateBuffer(
                    this.context.instance.troll.vmaAllocator(),
                    ciOperationBuffer, ciOperationAllocation,
                    pOperationBuffer, pOperationAllocation, operationAllocationInfo
                ), "vmaCreateBuffer"
            )
            this.operationVkBuffer = pOperationBuffer[0]
            this.operationBufferAllocation = pOperationAllocation[0]

            val operationBufferCpuAddress = operationAllocationInfo.pMappedData()
            this.operationCpuBuffer = memIntBuffer(operationBufferCpuAddress, ciOperationBuffer.size().toInt())
        }
    }

    fun destroy() {
        vmaDestroyBuffer(this.context.instance.troll.vmaAllocator(), this.operationVkBuffer, this.operationBufferAllocation)
        vmaDestroyBuffer(this.context.instance.troll.vmaAllocator(), this.vertexVkBuffer, this.vertexBufferAllocation)
    }
}
