package dragons.vulkan.init

import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME
import org.lwjgl.vulkan.KHRBindMemory2.VK_KHR_BIND_MEMORY_2_EXTENSION_NAME
import org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.slf4j.LoggerFactory.getLogger
import troll.exceptions.VulkanFailureException.assertVmaSuccess

internal fun initVma(
    vkInstance: VkInstance, vkPhysicalDevice: VkPhysicalDevice, vkDevice: VkDevice,
    enabledDeviceExtensions: Set<String>
): Long {
    val logger = getLogger("Vulkan")
    return stackPush().use { stack ->
        val vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack)
        vmaVulkanFunctions.set(vkInstance, vkDevice)

        var allocatorFlags = 0
        if (enabledDeviceExtensions.contains(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)) {
            allocatorFlags = allocatorFlags or VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT
            logger.info("The VMA allocator will be created with the VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT flag")
        }
        if (enabledDeviceExtensions.contains(VK_KHR_BIND_MEMORY_2_EXTENSION_NAME)) {
            allocatorFlags = allocatorFlags or VMA_ALLOCATOR_CREATE_KHR_BIND_MEMORY2_BIT
            logger.info("The VMA allocator will be created with the VMA_ALLOCATOR_CREATE_KHR_BIND_MEMORY2_BIT flag")
        }
        if (enabledDeviceExtensions.contains(VK_EXT_MEMORY_BUDGET_EXTENSION_NAME)) {
            allocatorFlags = allocatorFlags or VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT
            logger.info("The VMA allocator will be created with the VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT flag")
        }

        val ciAllocator = VmaAllocatorCreateInfo.calloc(stack)
        ciAllocator.flags(allocatorFlags)
        ciAllocator.vulkanApiVersion(VK_API_VERSION_1_0)
        ciAllocator.physicalDevice(vkPhysicalDevice)
        ciAllocator.device(vkDevice)
        ciAllocator.instance(vkInstance)
        ciAllocator.pVulkanFunctions(vmaVulkanFunctions)

        val pAllocator = stack.callocPointer(1)
        logger.info("Creating VMA allocator...")
        assertVmaSuccess(vmaCreateAllocator(ciAllocator, pAllocator), "CreateAllocator", null)
        logger.info("Created VMA allocator")
        pAllocator[0]
    }
}
