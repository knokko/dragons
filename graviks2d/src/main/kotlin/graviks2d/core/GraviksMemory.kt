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
}
