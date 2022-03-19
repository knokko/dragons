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
import java.util.*
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

    private fun withTestImage(context: GraviksContext, flipY: Boolean, test: (() -> Unit, HostImage) -> Unit) {
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

            test({ context.copyColorImageTo(destImage = null, destBuffer = testBuffer)}, HostImage(context.width, context.height, testHostBuffer, flipY))

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
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        withTestImage(graviks, true) { update, hostImage ->
            graviks.fillRect(0f, 0f, 0.75f, 0.5f, rectColor)
            update()
            assertColorEquals(rectColor, hostImage.getPixel(0, 0))
            assertColorEquals(rectColor, hostImage.getPixel(2, 1))
            assertColorEquals(rectColor, hostImage.getPixel(5, 3))
            assertColorEquals(backgroundColor, hostImage.getPixel(6, 0))
            assertColorEquals(backgroundColor, hostImage.getPixel(0, 4))
        }

        graviks.destroy()
    }

    @Test
    fun testAutomaticDepth() {
        val colors = (0 until 10).map { Color.rgbInt(10 * it, 10 * it, 10 * it) }
        val graviks = GraviksContext(
            this.graviksInstance, 1, 1, TranslucentPolicy.Manual, maxDepth = 2
        )

        withTestImage(graviks, true) { update, hostImage ->
            for (color in colors) {
                graviks.fillRect(0f, 0f, 1f, 1f, color)
                update()
                assertColorEquals(color, hostImage.getPixel(0, 0))
            }

            for (color in colors) {
                graviks.fillRect(0f, 0f, 1f, 1f, color)
            }
            update()
            assertColorEquals(colors[colors.size - 1], hostImage.getPixel(0, 0))
        }

        graviks.destroy()
    }

    @Test
    fun testDefaultDepthPrecision() {
        val rng = Random(12345)
        val size = 3000
        val invSize = 1f / size
        val numSquaresPerRow = (size - 2) / 2
        var totalNumSquares = numSquaresPerRow * numSquaresPerRow

        val graviks = GraviksContext(
            this.graviksInstance, size, size,
            TranslucentPolicy.Manual, depthPolicy = DepthPolicy.Manual,
            vertexBufferSize = 6 * totalNumSquares + 100,
            operationBufferSize = 2 * totalNumSquares + 100
        )

        if (totalNumSquares > graviks.maxDepth) {
            totalNumSquares = graviks.maxDepth
        }

        val depthFactor = graviks.maxDepth / totalNumSquares

        val colors = (0 until totalNumSquares).map { Color.rgbInt(rng.nextInt(255), rng.nextInt(255), rng.nextInt(255)) }

        withTestImage(graviks, true) { update, hostImage ->
            for (y in 0 until numSquaresPerRow) {
                for (x in (0 until numSquaresPerRow).reversed()) {
                    val colorIndex = x + numSquaresPerRow * y
                    if (colorIndex < colors.size) {
                        val factor = 2f * invSize
                        graviks.setManualDepth(1 + x + y * depthFactor * numSquaresPerRow)
                        graviks.fillRect(
                            factor * x,
                            factor * y,
                            factor * (x + 2),
                            factor * (y + 2),
                            colors[colorIndex]
                        )
                    }
                }
            }
            update()
            for (x in 0 until numSquaresPerRow) {
                for (y in 0 until numSquaresPerRow) {
                    val colorIndex = x + numSquaresPerRow * y
                    if (colorIndex < colors.size) {
                        val expectedColor = colors[colorIndex]
                        assertColorEquals(expectedColor, hostImage.getPixel(2 * x, 2 * y))
                        assertColorEquals(expectedColor, hostImage.getPixel(2 * x + 1, 2 * y))
                        assertColorEquals(expectedColor, hostImage.getPixel(2 * x, 2 * y + 1))
                        assertColorEquals(expectedColor, hostImage.getPixel(2 * x + 1, 2 * y + 1))
                    }
                }
            }
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