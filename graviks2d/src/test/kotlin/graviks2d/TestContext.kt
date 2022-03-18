package graviks2d

import graviks2d.context.DepthPolicy
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.core.GraviksInstance
import graviks2d.util.Color
import graviks2d.util.HostImage
import graviks2d.util.assertSuccess
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.absoluteValue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestContext {

    private val vkInstance: VkInstance
    private val vkPhysicalDevice: VkPhysicalDevice
    private val vkDevice: VkDevice
    private val vmaAllocator: Long
    private val graviksInstance: GraviksInstance

    init {
        stackPush().use { stack ->
            val ciInstance = VkInstanceCreateInfo.calloc(stack)
            ciInstance.`sType$Default`()
            ciInstance.ppEnabledLayerNames(stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation")))
            ciInstance.ppEnabledExtensionNames(stack.pointers(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME)))

            val pInstance = stack.callocPointer(1)
            assertSuccess(
                vkCreateInstance(ciInstance, null, pInstance),
                "vkCreateInstance"
            )
            this.vkInstance = VkInstance(pInstance[0], ciInstance)

            val pNumDevices = stack.ints(1)
            val pDevices = stack.callocPointer(1)
            assertSuccess(
                vkEnumeratePhysicalDevices(vkInstance, pNumDevices, pDevices),
                "vkEnumeratePhysicalDevices"
            )
            if (pNumDevices[0] < 1) {
                throw UnsupportedOperationException("At least 1 physical device is required, but got ${pNumDevices[0]}")
            }
            this.vkPhysicalDevice = VkPhysicalDevice(pDevices[0], vkInstance) // Just pick the first device

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
            this.vkDevice = VkDevice(pDevice[0], vkPhysicalDevice, ciDevice)

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
            this.vmaAllocator = pAllocator[0]

            this.graviksInstance = GraviksInstance(
                vkInstance, vkPhysicalDevice, vkDevice, vmaAllocator,
                graphicsQueueIndex
            ) { pSubmitInfo, fence ->
                vkQueueSubmit(queue, pSubmitInfo, fence)
            }
        }
    }

    private fun withTestImage(context: GraviksContext, flipY: Boolean, test: (Long, HostImage) -> Unit) {
        stackPush().use { stack ->

            val ciTestBuffer = VkBufferCreateInfo.calloc(stack)
            ciTestBuffer.`sType$Default`()
            ciTestBuffer.size((context.width * context.height * 4).toLong())
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
            val testHostBuffer = MemoryUtil.memByteBuffer(testAllocationInfo.pMappedData(), context.width * context.height * 4)

            test(testBuffer, HostImage(context.width, context.height, testHostBuffer, flipY))

            vmaDestroyBuffer(vmaAllocator, testBuffer, testAllocation)
        }
    }

    private fun assertColorEquals(expected: Color, actual: Color) {
        fun assertComponentEquals(expected: Int, actual: Int, description: String) {
            assertTrue((expected - actual).absoluteValue <= 2, "$description not equal: expected $expected, but got $actual")
        }
        assertComponentEquals(expected.red, actual.red, "red")
        assertComponentEquals(expected.green, actual.green, "green")
        assertComponentEquals(expected.blue, actual.blue, "blue")
        assertComponentEquals(expected.alpha, actual.alpha, "alpha")
    }

    @Test
    fun testFillRectangle() {
        val backgroundColor = Color.rgbInt(100, 0, 0)
        val rectColor = Color.rgbInt(0, 100, 0)
        val graviks = GraviksContext(
            this.graviksInstance, 8, 8,
            DepthPolicy.AlwaysIncrement, TranslucentPolicy.Manual, backgroundColor
        )

        withTestImage(graviks, true) { destBuffer, hostImage ->
            graviks.fillRect(0f, 0f, 0.75f, 0.5f, rectColor)
            graviks.copyColorImageTo(destImage = null, destBuffer = destBuffer)
            assertColorEquals(rectColor, hostImage.getPixel(0, 0))
            assertColorEquals(rectColor, hostImage.getPixel(2, 1))
            assertColorEquals(rectColor, hostImage.getPixel(5, 3))
            assertColorEquals(backgroundColor, hostImage.getPixel(6, 0))
            assertColorEquals(backgroundColor, hostImage.getPixel(0, 4))
        }

        graviks.destroy()
    }

    @AfterAll
    fun destroyGraviksInstance() {
        this.graviksInstance.destroy()
        vmaDestroyAllocator(this.vmaAllocator)
        vkDestroyDevice(this.vkDevice, null)
        vkDestroyInstance(this.vkInstance, null)
    }
}