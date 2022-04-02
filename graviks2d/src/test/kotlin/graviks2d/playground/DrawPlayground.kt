package graviks2d.playground

import graviks2d.context.DepthPolicy
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.core.GraviksInstance
import graviks2d.util.Color
import graviks2d.util.HostImage
import graviks2d.util.assertSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import java.io.File

fun main() {
    stackPush().use { stack ->
        val ciInstance = VkInstanceCreateInfo.calloc(stack)
        ciInstance.`sType$Default`()
        ciInstance.ppEnabledLayerNames(stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation")))
        ciInstance.ppEnabledExtensionNames(stack.pointers(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)))

        val pInstance = stack.callocPointer(1)
        assertSuccess(
            vkCreateInstance(ciInstance, null, pInstance),
            "vkCreateInstance"
        )
        val vkInstance = VkInstance(pInstance[0], ciInstance)

        val pNumDevices = stack.ints(1)
        val pDevices = stack.callocPointer(1)
        assertSuccess(
            vkEnumeratePhysicalDevices(vkInstance, pNumDevices, pDevices),
            "vkEnumeratePhysicalDevices"
        )
        if (pNumDevices[0] < 1) {
            throw UnsupportedOperationException("At least 1 physical device is required, but got ${pNumDevices[0]}")
        }
        val vkPhysicalDevice = VkPhysicalDevice(pDevices[0], vkInstance) // Just pick the first device

        val pNumQueueFamilies = stack.callocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pNumQueueFamilies, null)
        val numQueueFamilies = pNumQueueFamilies[0]

        val queueFamilies = VkQueueFamilyProperties.calloc(numQueueFamilies, stack)
        vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pNumQueueFamilies, queueFamilies)

        var graphicsQueueIndex: Int? = null
        for ((queueIndex, queue) in queueFamilies.withIndex()) {
            if ((queue.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0) {
                graphicsQueueIndex = queueIndex
                break
            }
        }

        val ciQueues = VkDeviceQueueCreateInfo.calloc(1, stack)
        val ciQueue = ciQueues[0]
        ciQueue.`sType$Default`()
        ciQueue.queueFamilyIndex(graphicsQueueIndex!!)
        ciQueue.pQueuePriorities(stack.floats(1f))

        val ciDevice = VkDeviceCreateInfo.calloc(stack)
        ciDevice.`sType$Default`()
        ciDevice.pQueueCreateInfos(ciQueues)

        val pDevice = stack.callocPointer(1)
        assertSuccess(
            vkCreateDevice(vkPhysicalDevice, ciDevice, null, pDevice),
            "vkCreateDevice"
        )
        val vkDevice = VkDevice(pDevice[0], vkPhysicalDevice, ciDevice)

        val pQueue = stack.callocPointer(1)
        vkGetDeviceQueue(vkDevice, graphicsQueueIndex, 0, pQueue)
        val queue = VkQueue(pQueue[0], vkDevice)

        val vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack)
        vmaVulkanFunctions.set(vkInstance, vkDevice)

        val ciAllocator = VmaAllocatorCreateInfo.calloc(stack)
        ciAllocator.vulkanApiVersion(VK_API_VERSION_1_0)
        ciAllocator.physicalDevice(vkPhysicalDevice)
        ciAllocator.device(vkDevice)
        ciAllocator.instance(vkInstance)
        ciAllocator.pVulkanFunctions(vmaVulkanFunctions)

        val pAllocator = stack.callocPointer(1)
        assertSuccess(
            vmaCreateAllocator(ciAllocator, pAllocator),
            "vmaCreateAllocator"
        )
        val vmaAllocator = pAllocator[0]

        val width = 1000
        val height = 700

        val ciTestBuffer = VkBufferCreateInfo.calloc(stack)
        ciTestBuffer.`sType$Default`()
        ciTestBuffer.size((width * height * 4).toLong())
        ciTestBuffer.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT)
        ciTestBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

        val ciTestAllocation = VmaAllocationCreateInfo.calloc(stack)
        ciTestAllocation.flags(
            VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT or
                    VMA_ALLOCATION_CREATE_MAPPED_BIT
        )
        ciTestAllocation.usage(VMA_MEMORY_USAGE_AUTO)

        val pTestBuffer = stack.callocLong(1)
        val pTestAllocation = stack.callocPointer(1)
        val testAllocationInfo = VmaAllocationInfo.calloc(stack)
        assertSuccess(
            vmaCreateBuffer(vmaAllocator, ciTestBuffer, ciTestAllocation, pTestBuffer, pTestAllocation, testAllocationInfo),
            "vmaCreateBuffer"
        )
        val testBuffer = pTestBuffer[0]
        val testAllocation = pTestAllocation[0]
        val testHostBuffer = memByteBuffer(testAllocationInfo.pMappedData(), width * height * 4)
        val testHostImage = HostImage(width, height, testHostBuffer, true)

        val graviksInstance = GraviksInstance(
            vkInstance, vkPhysicalDevice, vkDevice, vmaAllocator,
            graphicsQueueIndex, { pSubmitInfo, fence ->
                vkQueueSubmit(queue, pSubmitInfo, fence)
            }
        )

        val graviks = GraviksContext(
            graviksInstance, width, height,
            TranslucentPolicy.Manual,
            initialBackgroundColor = Color.rgbInt(200, 100, 150)
        )
//        graviks.fillRect(0.1f, 0.1f, 0.2f, 0.4f, Color.rgbInt(200, 0, 0))
//        graviks.fillRect(0.5f, 0.3f, 0.8f, 0.5f, Color.rgbInt(0, 200, 0))
//        graviks.fillRect(0.6f, 0.7f, 0.8f, 0.9f, Color.rgbInt(0, 0, 200))
        graviks.drawString(0.1f, 0.1f, 0.5f, 0.13f, "B", Color.rgbInt(0, 0, 200), Color.rgbInt(200, 0, 0))
        graviks.copyColorImageTo(destImage = null, destBuffer = testBuffer)
        testHostImage.saveToDisk(File("test1.png"))
//        graviks.fillRect(0.4f, 0.4f, 0.6f, 0.6f, Color.rgbInt(200, 50, 150))
//        graviks.copyColorImageTo(destImage = null, destBuffer = testBuffer)
//        testHostImage.saveToDisk(File("test2.png"))

        graviks.destroy()
        graviksInstance.destroy()

        vmaDestroyBuffer(vmaAllocator, testBuffer, testAllocation)
        vmaDestroyAllocator(vmaAllocator)
        vkDestroyDevice(vkDevice, null)
        vkDestroyInstance(vkInstance, null)
    }
}
