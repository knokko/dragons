package graviks2d.core

import graviks2d.pipeline.GraviksVertex
import graviks2d.pipeline.GraviksVertexBuffer
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo

internal class GraviksMemory(
    val instance: GraviksInstance
) {

    val vmaAllocator: Long

    init {
        vmaAllocator = stackPush().use { stack ->
            val vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack)
            vmaVulkanFunctions.set(instance.instance, instance.device)

            val ciAllocator = VmaAllocatorCreateInfo.calloc(stack)
            ciAllocator.vulkanApiVersion(VK_API_VERSION_1_0)
            ciAllocator.physicalDevice(instance.physicalDevice)
            ciAllocator.device(instance.device)
            ciAllocator.instance(instance.instance)
            ciAllocator.pVulkanFunctions(vmaVulkanFunctions)

            val pAllocator = stack.callocPointer(1)
            assertSuccess(vmaCreateAllocator(ciAllocator, pAllocator), "vmaCreateAllocator")
            pAllocator[0]
        }
    }

    fun createVertexBuffer(maxNumVertices: Int): GraviksVertexBufferWrapper {
        return stackPush().use { stack ->
            val ciBuffer = VkBufferCreateInfo.calloc(stack)
            ciBuffer.`sType$Default`()
            ciBuffer.size(maxNumVertices * GraviksVertex.BYTE_SIZE.toLong())
            if (ciBuffer.size() > Int.MAX_VALUE) {
                throw IllegalArgumentException("Too many vertices ($maxNumVertices)")
            }
            ciBuffer.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
            ciBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val ciAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciAllocation.flags(
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or
                        VMA_ALLOCATION_CREATE_MAPPED_BIT
            )
            ciAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val allocationInfo = VmaAllocationInfo.calloc(stack)

            val pBuffer = stack.callocLong(1)
            val pAllocation = stack.callocPointer(1)

            assertSuccess(
                vmaCreateBuffer(vmaAllocator, ciBuffer, ciAllocation, pBuffer, pAllocation, allocationInfo),
                "vmaCreateBuffer"
            )

            val cpuAddress = allocationInfo.pMappedData()
            val rawCpuBuffer = memByteBuffer(cpuAddress, ciBuffer.size().toInt())
            val cpuBuffer = GraviksVertexBuffer.createAtBuffer(rawCpuBuffer, maxNumVertices)

            GraviksVertexBufferWrapper(
                cpuBuffer = cpuBuffer,
                vkBuffer = pBuffer[0],
                vmaAllocation = pAllocation[0]
            )
        }
    }

    fun destroy() {
        vmaDestroyAllocator(vmaAllocator)
    }
}

internal class GraviksVertexBufferWrapper(
    val cpuBuffer: GraviksVertexBuffer,
    val vkBuffer: Long,
    val vmaAllocation: Long
) {
    fun destroy(allocator: Long) {
        vmaDestroyBuffer(allocator, vkBuffer, vmaAllocation)
    }
}
