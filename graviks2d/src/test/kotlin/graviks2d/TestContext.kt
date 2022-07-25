package graviks2d

import graviks2d.context.DepthPolicy
import graviks2d.context.GraviksContext
import graviks2d.context.TranslucentPolicy
import graviks2d.core.GraviksInstance
import graviks2d.resource.image.ImageCache
import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.TextStyle
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
import org.lwjgl.system.MemoryUtil.*
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
    private val graphicsQueueIndex: Int
    private val queue: VkQueue
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
            this.graphicsQueueIndex = graphicsQueueIndex!!

            val ciQueues = VkDeviceQueueCreateInfo.calloc(1, stack)
            val ciQueue = ciQueues[0]
            ciQueue.`sType$Default`()
            ciQueue.queueFamilyIndex(this.graphicsQueueIndex)
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
            vkGetDeviceQueue(vkDevice, this.graphicsQueueIndex, 0, pQueue)
            this.queue = VkQueue(pQueue[0], vkDevice)

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
                this.graphicsQueueIndex, queueSubmit = { pSubmitInfo, fence ->
                    synchronized(this.queue) {
                        vkQueueSubmit(queue, pSubmitInfo, fence)
                    }
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
            val testHostBuffer = memByteBuffer(testAllocationInfo.pMappedData(), context.width * context.height * 4)

            test({ context.copyColorImageTo(
                destImage = null, destBuffer = testBuffer, destImageFormat = null
            )}, HostImage(context.width, context.height, testHostBuffer, flipY))

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
    fun testFillRoundedRect() {
        val backgroundColor = Color.rgbInt(100, 0, 0)
        val graviks = GraviksContext(
            this.graviksInstance, 100, 100,
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        withTestImage(graviks, true) { update, hostImage ->
            val rectColor = Color.rgbInt(0, 100, 0)
            graviks.fillRoundedRect(0.1f, 0.2f, 0.6f, 0.4f, 0.1f, rectColor)
            update()

            // Left side
            assertColorEquals(backgroundColor, hostImage.getPixel(11, 21))
            assertColorEquals(backgroundColor, hostImage.getPixel(11, 38))
            for (x in 11 until 23) assertColorEquals(rectColor, hostImage.getPixel(x, 30))

            // Middle
            assertColorEquals(backgroundColor, hostImage.getPixel(35, 18))
            for (y in 21 .. 38) assertColorEquals(rectColor, hostImage.getPixel(35, y))
            assertColorEquals(backgroundColor, hostImage.getPixel(35, 41))

            // Right
            assertColorEquals(backgroundColor, hostImage.getPixel(58, 21))
            assertColorEquals(backgroundColor, hostImage.getPixel(58, 38))
            for (x in 48 until 58) assertColorEquals(rectColor, hostImage.getPixel(x, 30))
        }

        graviks.destroy()
    }

    @Test
    fun testDrawRoundedRect() {
        val backgroundColor = Color.rgbInt(100, 0, 0)
        val graviks = GraviksContext(
            this.graviksInstance, 100, 100,
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        withTestImage(graviks, true) { update, hostImage ->
            val rectColor = Color.rgbInt(0, 100, 0)
            graviks.drawRoundedRect(0.1f, 0.2f, 0.6f, 0.4f, 0.1f, 0.55f, rectColor)
            update()

            // Left side
            assertColorEquals(backgroundColor, hostImage.getPixel(11, 21))
            assertColorEquals(backgroundColor, hostImage.getPixel(11, 38))
            assertColorEquals(rectColor, hostImage.getPixel(11, 30))
            for (x in 16 until 23) assertColorEquals(backgroundColor, hostImage.getPixel(x, 30))

            // Middle
            assertColorEquals(backgroundColor, hostImage.getPixel(35, 18))
            assertColorEquals(rectColor, hostImage.getPixel(35, 21))
            assertColorEquals(backgroundColor, hostImage.getPixel(35, 30))
            assertColorEquals(rectColor, hostImage.getPixel(35, 38))
            assertColorEquals(backgroundColor, hostImage.getPixel(35, 41))

            // Right
            assertColorEquals(backgroundColor, hostImage.getPixel(58, 21))
            assertColorEquals(backgroundColor, hostImage.getPixel(58, 38))
            assertColorEquals(rectColor, hostImage.getPixel(58, 30))
            for (x in 48 until 54) assertColorEquals(backgroundColor, hostImage.getPixel(x, 30))
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
    fun testDrawString() {
        val backgroundColor = Color.rgbInt(255, 255, 255)
        val graviks = GraviksContext(
            this.graviksInstance, 300, 600,
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        val style = TextStyle(
            fillColor = Color.rgbInt(0, 0, 150),
            font = null
        )

        withTestImage(graviks, true) { update, hostImage ->
            // Test that drawing an empty string won't crash and has no effect
            graviks.drawString(0.1f, 0.2f, 0.3f, 0.4f, "", style, backgroundColor)
            update()
            for (x in 0 until hostImage.width) {
                for (y in 0 until hostImage.height) {
                    assertColorEquals(backgroundColor, hostImage.getPixel(x, y))
                }
            }

            graviks.drawString(0.1f, 0.1f, 0.9f, 0.4f, "T E", style, backgroundColor)
            update()

            fun countSplittedOccurrences(x: Int): Int {
                var counter = 0
                var wasTextColor = false
                for (y in 0 until graviks.height) {
                    val isTextColor = hostImage.getPixel(x, y) == style.fillColor
                    if (isTextColor && !wasTextColor) counter += 1
                    wasTextColor = isTextColor
                }
                return counter
            }

            fun countAllOccurrences(x: Int): Int {
                return (0 until graviks.height).count { y -> hostImage.getPixel(x, y) == style.fillColor }
            }

            val tBarLeftIndex = (0 until graviks.width).indexOfFirst { x -> countAllOccurrences(x) > 50 }
            val tBarRightIndex = (0 until graviks.width).indexOfFirst { x -> x > tBarLeftIndex && countAllOccurrences(x) < 50 } - 1
            assertEquals(2, countSplittedOccurrences(tBarLeftIndex - 2))
            assertEquals(2, countSplittedOccurrences(tBarRightIndex + 2))
            assertEquals(1, countSplittedOccurrences(tBarLeftIndex - 25))
            assertEquals(1, countSplittedOccurrences(tBarRightIndex + 25))

            val eBarRightIndex = (0 until graviks.width).indexOfLast { x -> countAllOccurrences(x) > 50 }
            val eBarLeftIndex = (0 until eBarRightIndex).indexOfLast { x -> countAllOccurrences(x) < 50 } + 1
            assertEquals(0, countSplittedOccurrences(eBarLeftIndex - 20))
            assertEquals(2, countSplittedOccurrences(eBarLeftIndex - 2))
            assertEquals(3, countSplittedOccurrences(eBarRightIndex + 2))
            assertEquals(3, countSplittedOccurrences(eBarRightIndex + 20))

            for (x in 0 until graviks.width) {
                for (y in graviks.height / 2 until graviks.height) {
                    assertColorEquals(backgroundColor, hostImage.getPixel(x, y))
                }
            }

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

    @Test
    fun testDrawImageDescriptorManagement() {
        val backgroundColor = Color.rgbInt(255, 255, 255)

        val graviksInstance = GraviksInstance(
            vkInstance, vkPhysicalDevice, vkDevice, vmaAllocator,
            graphicsQueueIndex, maxNumDescriptorImages = 2, queueSubmit = { pSubmitInfo, fence ->
                vkQueueSubmit(queue, pSubmitInfo, fence)
            }
        )

        val graviks = GraviksContext(
            graviksInstance, 20, 20,
            TranslucentPolicy.Manual, initialBackgroundColor = backgroundColor
        )

        val colors = (0 until 20).map { Color.rgbInt(10 * it, 5 * it, it) }
        val images = colors.map { color ->
            val bufferedImage = BufferedImage(1, 1, TYPE_INT_ARGB)
            bufferedImage.setRGB(0, 0, java.awt.Color(color.red, color.green, color.blue).rgb)

            val file = Files.createTempFile(null, ".png").toFile()
            ImageIO.write(bufferedImage, "PNG", file)

            ImageReference.file(file)
        }

        withTestImage(graviks, true) { update, hostImage ->
            for ((index, image) in images.withIndex()) {
                graviks.drawImage(index / 20f, index / 20f, 1f, 1f, image)
            }
            update()
            for ((index, color) in colors.withIndex()) {
                for (x in index until 20) {
                    assertColorEquals(color, hostImage.getPixel(x, index))
                }
                for (y in index until 20) {
                    assertColorEquals(color, hostImage.getPixel(index, y))
                }
            }
        }

        graviks.destroy()
        graviksInstance.destroy()
    }

    @Test
    fun testGetImageSize() {
        val graviks = GraviksContext(
            this.graviksInstance, 50, 50,
            TranslucentPolicy.Manual, initialBackgroundColor = Color.rgbInt(1, 2, 3)
        )

        assertEquals(
            Pair(14, 14),
            graviks.getImageSize(ImageReference.classLoaderPath("graviks2d/images/test1.png", false))
        )

        val testFile = Files.createTempFile("", ".png").toFile()
        val testImage = BufferedImage(10, 11, TYPE_INT_ARGB)
        ImageIO.write(testImage, "PNG", testFile)

        assertEquals(Pair(10, 11), graviks.getImageSize(ImageReference.file(testFile)))

        graviks.destroy()
    }

    @Test
    fun testGetStringAspectRatio() {
        val graviks = GraviksContext(
            this.graviksInstance, 50, 50,
            TranslucentPolicy.Manual, initialBackgroundColor = Color.rgbInt(1, 2, 3)
        )

        val aspectHelloWorld = graviks.getStringAspectRatio("Hello, World!", null)
        assertEquals(5.11f, aspectHelloWorld, 0.5f)
        val aspectH = graviks.getStringAspectRatio("H", null)
        assertEquals(0.58f, aspectH, 0.2f)
        assertEquals(0f, graviks.getStringAspectRatio("", null))

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

    @Test
    fun testBlitColorImageTo() {
        val graviks = GraviksContext(
            this.graviksInstance, 50, 50,
            TranslucentPolicy.Manual, initialBackgroundColor = Color.rgbInt(10, 20, 30)
        )

        stackPush().use { stack ->
            val ciDestImage = VkImageCreateInfo.calloc(stack)
            ciDestImage.`sType$Default`()
            ciDestImage.imageType(VK_IMAGE_TYPE_2D)
            ciDestImage.format(VK_FORMAT_B8G8R8A8_UNORM)
            ciDestImage.extent().set(graviks.width, graviks.height, 1)
            ciDestImage.mipLevels(1)
            ciDestImage.arrayLayers(1)
            ciDestImage.samples(VK_SAMPLE_COUNT_1_BIT)
            ciDestImage.tiling(VK_IMAGE_TILING_OPTIMAL)
            ciDestImage.usage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)
            ciDestImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            ciDestImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

            val ciDestImageAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciDestImageAllocation.usage(VMA_MEMORY_USAGE_AUTO_PREFER_HOST)

            val pDestImage = stack.callocLong(1)
            val pDestImageAllocation = stack.callocPointer(1)
            assertSuccess(
                vmaCreateImage(
                    vmaAllocator, ciDestImage, ciDestImageAllocation,
                    pDestImage, pDestImageAllocation, null
                ), "vmaCreateImage"
            )
            val destImage = pDestImage[0]
            val destImageAllocation = pDestImageAllocation[0]

            graviks.copyColorImageTo(
                destImage = destImage, destBuffer = null, destImageFormat = VK_FORMAT_B8G8R8A8_UNORM,
                originalImageLayout = VK_IMAGE_LAYOUT_UNDEFINED, finalImageLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                imageSrcAccessMask = 0, imageSrcStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                imageDstAccessMask = VK_ACCESS_TRANSFER_READ_BIT, imageDstStageMask = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT
            )

            val ciDestBuffer = VkBufferCreateInfo.calloc(stack)
            ciDestBuffer.`sType$Default`()
            ciDestBuffer.size((graviks.width * graviks.height * 4).toLong())
            ciDestBuffer.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT)
            ciDestBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val ciDestBufferAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciDestBufferAllocation.flags(
                VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT or
                        VMA_ALLOCATION_CREATE_MAPPED_BIT
            )
            ciDestBufferAllocation.usage(VMA_MEMORY_USAGE_AUTO)

            val pDestBuffer = stack.callocLong(1)
            val pDestBufferAllocation = stack.callocPointer(1)
            val destBufferAllocationInfo = VmaAllocationInfo.calloc(stack)
            assertSuccess(
                vmaCreateBuffer(vmaAllocator, ciDestBuffer, ciDestBufferAllocation, pDestBuffer, pDestBufferAllocation, destBufferAllocationInfo),
                "vmaCreateBuffer"
            )
            val destBuffer = pDestBuffer[0]
            val destBufferAllocation = pDestBufferAllocation[0]
            val destHostBuffer = memByteBuffer(destBufferAllocationInfo.pMappedData(), graviks.width * graviks.height * 4)

            val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
            ciCommandPool.`sType$Default`()
            ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
            ciCommandPool.queueFamilyIndex(graphicsQueueIndex)

            val pCommandPool = stack.callocLong(1)
            assertSuccess(
                vkCreateCommandPool(graviks.instance.device, ciCommandPool, null, pCommandPool),
                "vkCreateCommandPool"
            )
            val commandPool = pCommandPool[0]

            val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
            aiCommandBuffer.`sType$Default`()
            aiCommandBuffer.commandPool(commandPool)
            aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            aiCommandBuffer.commandBufferCount(1)

            val pCommandBuffer = stack.callocPointer(1)
            assertSuccess(
                vkAllocateCommandBuffers(graviks.instance.device, aiCommandBuffer, pCommandBuffer),
                "vkAllocateCommandBuffers"
            )
            val commandBuffer = VkCommandBuffer(pCommandBuffer[0], graviks.instance.device)

            val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
            biCommandBuffer.`sType$Default`()
            assertSuccess(vkBeginCommandBuffer(commandBuffer, biCommandBuffer), "vkBeginCommandBuffer")

            val copyRegions = VkBufferImageCopy.calloc(1, stack)
            val copyRegion = copyRegions[0]
            copyRegion.imageSubresource {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseArrayLayer(0)
                it.layerCount(1)
                it.mipLevel(0)
            }
            copyRegion.imageExtent().set(graviks.width, graviks.height, 1)

            vkCmdCopyImageToBuffer(commandBuffer, destImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, destBuffer, copyRegions)

            val bufferBarriers = VkBufferMemoryBarrier.calloc(1, stack)
            val bufferBarrier = bufferBarriers[0]
            bufferBarrier.`sType$Default`()
            bufferBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            bufferBarrier.dstAccessMask(VK_ACCESS_HOST_READ_BIT)
            bufferBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            bufferBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            bufferBarrier.buffer(destBuffer)
            bufferBarrier.size(VK_WHOLE_SIZE)

            vkCmdPipelineBarrier(
                commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_HOST_BIT, 0,
                null, bufferBarriers, null
            )

            assertSuccess(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer")

            val siCopy = VkSubmitInfo.calloc(stack)
            siCopy.`sType$Default`()
            siCopy.waitSemaphoreCount(0)
            siCopy.pCommandBuffers(pCommandBuffer)
            siCopy.pSignalSemaphores(null)

            val ciCopyFence = VkFenceCreateInfo.calloc(stack)
            ciCopyFence.`sType$Default`()

            val pCopyFence = stack.callocLong(1)
            assertSuccess(
                vkCreateFence(graviks.instance.device, ciCopyFence, null, pCopyFence),
                "vkCreateFence"
            )
            val copyFence = pCopyFence[0]

            assertSuccess(vkQueueSubmit(queue, siCopy, copyFence), "vkQueueSubmit")
            assertSuccess(
                vkWaitForFences(graviks.instance.device, pCopyFence, true, 1_000_000_000),
                "vkWaitForFences"
            )

            vkDestroyFence(graviks.instance.device, copyFence, null)
            vkDestroyCommandPool(graviks.instance.device, commandPool, null)

            for (x in 0 until graviks.width) {
                for (y in 0 until graviks.height) {
                    val index = 4 * (x + y * graviks.width)
                    assertEquals(30, destHostBuffer[index].toUByte().toInt())
                    assertEquals(20, destHostBuffer[index + 1].toUByte().toInt())
                    assertEquals(10, destHostBuffer[index + 2].toUByte().toInt())
                    assertEquals(255, destHostBuffer[index + 3].toUByte().toInt())
                }
            }

            vmaDestroyImage(vmaAllocator, destImage, destImageAllocation)
            vmaDestroyBuffer(vmaAllocator, destBuffer, destBufferAllocation)
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
