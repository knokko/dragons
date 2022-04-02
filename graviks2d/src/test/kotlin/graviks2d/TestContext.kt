package graviks2d

import graviks2d.context.DepthPolicy
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.core.GraviksInstance
import graviks2d.resource.image.ImageCache
import graviks2d.resource.image.ImageReference
import graviks2d.util.Color
import graviks2d.util.HostImage
import graviks2d.util.assertSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.stb.STBImage.stbi_info_from_memory
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.opentest4j.AssertionFailedError
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.nio.file.Files
import java.util.*
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
                graphicsQueueIndex, queueSubmit = { pSubmitInfo, fence ->
                    vkQueueSubmit(queue, pSubmitInfo, fence)
                }
            )
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

    private fun assertImageEquals(expectedFileName: String, actual: HostImage) {
        val expectedInput = this::class.java.classLoader.getResourceAsStream("graviks2d/images/expected/$expectedFileName")!!
        val expectedByteArray = expectedInput.readAllBytes()
        expectedInput.close()

        val expectedByteBuffer = memCalloc(expectedByteArray.size)
        expectedByteBuffer.put(0, expectedByteArray)

        stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            val pChannels = stack.callocInt(1)

            assertTrue(stbi_info_from_memory(expectedByteBuffer, pWidth, pHeight, pChannels))
            val expectedHostBuffer = stbi_load_from_memory(expectedByteBuffer, pWidth, pHeight, pChannels, 4)!!

            val expectedHostImage = HostImage(pWidth[0], pHeight[0], expectedHostBuffer, true)
            assertImageEquals(expectedHostImage, actual)
            memFree(expectedHostBuffer)
        }
        memFree(expectedByteBuffer)
    }

    private fun assertImageEquals(expected: HostImage, actual: HostImage) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)

        try {
            for (x in 0 until expected.width) {
                for (y in 0 until expected.height) {
                    assertColorEquals(expected.getPixel(x, y), actual.getPixel(x, y))
                }
            }
        } catch (failed: AssertionFailedError) {

            // This should make debugging a lot easier
            val time = System.nanoTime()
            expected.saveToDisk(File("expected-$time.png"))
            actual.saveToDisk(File("actual-$time.png"))

            throw failed
        }
    }

    private fun assertColorEquals(expected: Color, actual: Color) {
        fun assertComponentEquals(expectedValue: Int, actualValue: Int) {
            assertTrue(
                (expectedValue - actualValue).absoluteValue <= 2,
                "expected (${expected.red},${expected.green},${expected.blue},${expected.alpha}) " +
                        "but got (${actual.red},${actual.green},${actual.blue},${actual.alpha})"
            )
        }
        assertComponentEquals(expected.red, actual.red)
        assertComponentEquals(expected.green, actual.green)
        assertComponentEquals(expected.blue, actual.blue)
        assertComponentEquals(expected.alpha, actual.alpha)
    }

    @Test
    fun testFillRectangle() {
        val backgroundColor = Color.rgbInt(100, 0, 0)
        val graviks = GraviksContext(
            this.graviksInstance, 20, 20,
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        withTestImage(graviks, false) { update, hostImage ->
            graviks.fillRect(0.15f, 0.35f, 0.35f, 0.8f, Color.rgbInt(50, 100, 0))
            graviks.fillRect(0.7f, 0.4f, 0.25f, 0.2f, Color.rgbInt(0, 100, 50))
            graviks.fillRect(0.8f, 0.3f, 0.85f, 0.9f, Color.rgbInt(0, 150, 250))
            graviks.fillRect(0f, 0.85f, 0.5f, 0.7f, Color.rgbInt(200, 200, 0))
            graviks.fillRect(0.95f, 0.85f, 0.3f, 0.9f, Color.rgbInt(0, 0, 0))
            graviks.fillRect(0.4f, 0.55f, 0.45f, 0.6f, Color.rgbInt(200, 0, 200))
            update()
            assertImageEquals("fillRectangle.png", hostImage)
        }

        graviks.destroy()
    }

    @Test
    fun testDrawImage() {
        val backgroundColor = Color.rgbInt(255, 255, 255)
        val graviks = GraviksContext(
            this.graviksInstance, 50, 50,
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        val image1 = ImageReference.classLoaderPath("graviks2d/images/test1.png", false)
        val image2 = ImageReference.classLoaderPath("graviks2d/images/test2.png", false)

        withTestImage(graviks, true) { update, hostImage ->
            fun drawFlippedImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
                graviks.drawImage(xLeft, 1f - yTop, xRight, 1f - yBottom, image)
            }

            drawFlippedImage(0.04f, 0.06f, 0.46f, 0.48f, image2)
            drawFlippedImage(0.14f, 0.34f, 0.56f, 0.76f, image2)
            drawFlippedImage(0.6f, 0.58f, 0.88f, 0.86f, image1)
            drawFlippedImage(0.32f, 0.22f, 0.6f, 0.5f, image1)
            drawFlippedImage(0.54f, 0.22f, 0.96f, 0.64f, image2)
            update()
            assertImageEquals("drawImage.png", hostImage)
        }

        graviks.destroy()
    }

    @Test
    fun testImageCache() {
        runBlocking {
            val cache = ImageCache(graviksInstance, softImageLimit = 2)

            val testImageFile = Files.createTempFile(null, null).toFile()
            ImageIO.write(BufferedImage(4, 6, TYPE_INT_ARGB), "PNG", testImageFile)
            val image10 = ImageReference.file(testImageFile)
            val image11 = ImageReference.file(testImageFile)

            val image20 = ImageReference.classLoaderPath("graviks2d/images/test1.png", false)
            val image21 = ImageReference.classLoaderPath("graviks2d/images/test1.png", false)

            val image30 = ImageReference.classLoaderPath("graviks2d/images/test2.png", false)

            // Initially, the cache should be empty
            assertEquals(0, cache.getCurrentCacheSize())

            // After borrowing 1 image, the cache should have size 1
            val borrowed10 = cache.borrowImage(image10)
            assertEquals(1, cache.getCurrentCacheSize())

            // Borrowing the same image again should not increase the cache size
            val borrowed11 = cache.borrowImage(image11)
            assertEquals(1, cache.getCurrentCacheSize())

            // Awaiting should not increase the cache size either
            borrowed10.imagePair.await()
            borrowed11.imagePair.await()
            assertEquals(1, cache.getCurrentCacheSize())

            // Borrowing another image should increase the cache size
            val borrowed20 = cache.borrowImage(image20)
            val borrowed21 = cache.borrowImage(image21)
            assertEquals(2, cache.getCurrentCacheSize())

            // Returning the other images should not decrease the cache size
            // when the image limit is not reached
            cache.returnImage(borrowed20)

            // Awaiting the image before returning is optional
            borrowed21.imagePair.await()
            cache.returnImage(borrowed21)
            assertEquals(2, cache.getCurrentCacheSize())

            // The second image should be removed from the cache when adding
            // a third image to the cache
            val borrowed30 = cache.borrowImage(image30)
            assertEquals(2, cache.getCurrentCacheSize())

            // When borrowing the second image again, the cache limit must be
            // exceeded because all images are still in use
            val borrowed22 = cache.borrowImage(image21)
            assertEquals(3, cache.getCurrentCacheSize())

            // But the third image should be removed as soon as it is returned
            // since the cache limit is already exceeded
            cache.returnImage(borrowed30)
            assertEquals(2, cache.getCurrentCacheSize())

            // Let's return all borrowed images
            cache.returnImage(borrowed10)
            cache.returnImage(borrowed11)
            cache.returnImage(borrowed22)

            // Since the cache limit is no longer exceeded, the size should remain 2
            assertEquals(2, cache.getCurrentCacheSize())

            // But destroying the cache should set the size to 0
            cache.destroy()
            assertEquals(0, cache.getCurrentCacheSize())
        }
    }

    // TODO Test the image descriptor management

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
            val time1 = System.currentTimeMillis()
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
            val time2 = System.currentTimeMillis()
            update()
            val time3 = System.currentTimeMillis()
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
            val time4 = System.currentTimeMillis()

            println("Performance of depth precision test: ${time2 - time1}, ${time3 - time2}, ${time4 - time3}")
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
