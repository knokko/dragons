package graviks2d.playground

import graviks2d.context.GraviksContext
import graviks2d.core.GraviksInstance
import graviks2d.resource.text.TextAlignment
import graviks2d.resource.text.TextOverflowPolicy
import graviks2d.resource.text.TextStyle
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
        val height = 4700

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
        val testHostImage = HostImage(width, height, testHostBuffer, false)

        val graviksInstance = GraviksInstance(
            vkInstance, vkPhysicalDevice, vkDevice, vmaAllocator,
            graphicsQueueIndex, { pSubmitInfo, fence ->
                vkQueueSubmit(queue, pSubmitInfo, fence)
            }
        )

        val graviks = GraviksContext(
            graviksInstance, width, height, initialBackgroundColor = Color.rgbInt(200, 100, 150),
        )

        val fillColor = Color.rgbInt(0, 200, 0)
        val strokeColor = Color.BLACK
        val time1 = System.currentTimeMillis()
        val minX = 0.05f
        graviks.fillRect(0f, 0f, minX, 1f, Color.BLACK)
        graviks.fillRect(0.8f, 0f, 1f, 1f, Color.BLACK)
        graviks.fillRect(0f, 0.99f, 1f, 1f, Color.WHITE)

        val infoStyle = TextStyle(
            font = null, fillColor = fillColor, strokeColor = strokeColor,
            alignment = TextAlignment.Natural, overflowPolicy = TextOverflowPolicy.DiscardEnd
        )
        val dummyTextEnglishShort = "Hello, world!"
        val dummyTextEnglishLong = "The quick brown fox jumps over the lazy dog"
        val dummyTextHebrewShort = "יוליאניש"
        val dummyTextHebrewLong = "געהייסן די Lord Chamberlain's Men ((לארד ט(שעמבע)רליינס מען)))"
        val styleVariations = TextAlignment.values().flatMap {
                alignment -> TextOverflowPolicy.values().map { overflow -> Pair(alignment, overflow) }
        }
        for ((index, styleVariation) in styleVariations.withIndex()) {
            val deltaY = 0.04f
            val minY = index * deltaY
            val maxY = (index + 1) * deltaY

            val infoString = "${styleVariation.first} - ${styleVariation.second}"
            graviks.drawString(
                minX, minY + 0.42f * deltaY, 0.4f, maxY - 0.42f * deltaY,
                infoString, infoStyle
            )
            val currentStyle = infoStyle.createChild(alignment = styleVariation.first, overflowPolicy = styleVariation.second)
            graviks.drawString(0.4f, minY, 0.8f, minY + 0.25f * deltaY, dummyTextEnglishShort, currentStyle)
            graviks.drawString(0.4f, minY + 0.25f * deltaY, 0.8f, minY + 0.5f * deltaY, dummyTextEnglishLong, currentStyle)
            graviks.drawString(0.4f, minY + 0.5f * deltaY, 0.8f, minY + 0.75f * deltaY, dummyTextHebrewShort, currentStyle)
            graviks.drawString(0.4f, minY + 0.75f * deltaY, 0.8f, maxY, dummyTextHebrewLong, currentStyle)
        }
        graviks.drawString(0.4f, 0.95f, 0.8f, 0.983f, "test1234", infoStyle)
        graviks.drawString(
            0.4f, 0.86f, 0.8f, 0.9f, "Without",
            infoStyle.createChild(strokeColor = infoStyle.fillColor)
        )
        graviks.drawString(
            0.4f, 0.84f, 0.8f, 0.843f, "Without",
            infoStyle.createChild(strokeColor = infoStyle.fillColor)
        )
        val time2 = System.currentTimeMillis()
        graviks.copyColorImageTo(destImage = null, destBuffer = testBuffer, destImageFormat = null, shouldAwaitCompletion = true)
        val time3 = System.currentTimeMillis()
        println("Took ${time2 - time1} ms and ${time3 - time2} ms")
        testHostImage.saveToDisk(File("test1.png"))

        graviks.destroy()
        graviksInstance.destroy()

        vmaDestroyBuffer(vmaAllocator, testBuffer, testAllocation)
        vmaDestroyAllocator(vmaAllocator)
        vkDestroyDevice(vkDevice, null)
        vkDestroyInstance(vkInstance, null)
    }
}
