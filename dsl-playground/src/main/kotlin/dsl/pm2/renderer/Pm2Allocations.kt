package dsl.pm2.renderer

import dsl.pm2.interpreter.Pm2Vertex
import dsl.pm2.renderer.pipeline.STATIC_VERTEX_SIZE
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkDevice

class Pm2Allocations internal constructor(
    private val vkDevice: VkDevice,
    private val vmaAllocator: Long,
    private val queueFamilyIndex: Int
) {

    fun allocateMesh(vertices: List<Pm2Vertex>): Pm2Mesh {
        // TODO Buffer sub-allocation
        return stackPush().use { stack ->
            val ciBuffer = VkBufferCreateInfo.calloc(stack)
            ciBuffer.`sType$Default`()
            ciBuffer.flags(0)
            ciBuffer.size(vertices.size * STATIC_VERTEX_SIZE.toLong())
            ciBuffer.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
            ciBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val ciStagingAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciStagingAllocation.flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or VMA_ALLOCATION_CREATE_MAPPED_BIT)
            ciStagingAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val pStagingBuffer = stack.callocLong(1)
            val pStagingAllocation = stack.callocPointer(1)

            val allocationInfo = VmaAllocationInfo.calloc(stack)

            checkReturnValue(vmaCreateBuffer(
                vmaAllocator, ciBuffer, ciStagingAllocation, pStagingBuffer, pStagingAllocation, allocationInfo
            ), "VmaCreateBuffer")

            val byteBuffer = memByteBuffer(allocationInfo.pMappedData(), vertices.size * STATIC_VERTEX_SIZE)
            for (vertex in vertices) {
                byteBuffer.putFloat(vertex.x)
                byteBuffer.putFloat(vertex.y)
                byteBuffer.putFloat(vertex.color.redF)
                byteBuffer.putFloat(vertex.color.greenF)
                byteBuffer.putFloat(vertex.color.blueF)
            }

            Pm2Mesh(
                vmaAllocation = pStagingAllocation[0], vkBuffer = pStagingBuffer[0],
                vertexOffset = 0, numVertices = vertices.size
            )
        }

    }

    fun destroyMesh(mesh: Pm2Mesh) {
        vmaDestroyBuffer(vmaAllocator, mesh.vkBuffer, mesh.vmaAllocation)
    }

    fun destroy() {

    }
}
