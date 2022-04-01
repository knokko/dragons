package graviks2d.playground

import graviks2d.font.TrueTypeFont
import graviks2d.pipeline.text.*
import graviks2d.util.assertSuccess
import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.util.vma.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import java.awt.BasicStroke
import java.awt.Stroke
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.DataInputStream
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import javax.imageio.ImageIO

private class Point(
    val x: Float,
    val y: Float
)

private class CurveSegment(
    val startPoint: Point,
    val controlPoint: Point,
    val endPoint: Point
)

private class VectorShape(
    val curveSegments: Array<CurveSegment>
)

private fun createCurveSegments(
    rawData: Array<Float>
): Array<CurveSegment> {
    if (rawData.size % 2 != 0) throw IllegalArgumentException("Size of rawData must be even")
    val rawPoints = (0 until rawData.size / 2).map { Point(rawData[2 * it], rawData[2 * it + 1]) }

    if ((rawPoints.size - 4) % 2 != 0) throw IllegalArgumentException("#points must be equal to 3 + 2n for some integer n")
    val numSegments = 2 + (rawPoints.size - 4) / 2

    return Array(numSegments) { CurveSegment(rawPoints[2 * it], rawPoints[2 * it + 1], rawPoints[(2 * it + 2) % rawPoints.size]) }
}

private val shapeE = VectorShape(
    createCurveSegments(arrayOf(
        0.24f, 0.43f,   0.22f, 0.39f,   0.22f, 0.3f,
        0.22f, 0.21f,   0.27f, 0.16f,
        0.33f, 0.11f,   0.42f, 0.11f,
        0.49f, 0.11f,   0.56f, 0.14f,
        0.62f, 0.17f,   0.74f, 0.27f,
        0.75f, 0.25f,   0.76f, 0.24f,
        0.67f, 0.13f,   0.55f, 0.07f,
        0.43f, 0.01f,   0.32f, 0.01f,
        0.19f, 0.01f,   0.11f, 0.09f,
        0.03f, 0.17f,   0.03f, 0.3f,
        0.03f, 0.46f,   0.13f, 0.62f,
        0.23f, 0.78f,   0.38f, 0.88f,
        0.53f, 0.98f,   0.68f, 0.98f,
        0.76f, 0.98f,   0.81f, 0.94f,
        0.85f, 0.91f,   0.85f, 0.84f,
        0.85f, 0.7f,    0.7f, 0.59f,
        0.54f, 0.48f,   0.3f, 0.44f,
        0.27f, 0.44f
    )) +
    createCurveSegments(arrayOf(
        0.26f, 0.48f,   0.27f, 0.53f,   0.29f, 0.58f,
        0.35f, 0.72f,   0.45f, 0.83f,
        0.55f, 0.93f,   0.64f, 0.93f,
        0.67f, 0.93f,   0.69f, 0.91f,
        0.71f, 0.89f,   0.71f, 0.85f,
        0.71f, 0.82f,   0.7f, 0.79f,
        0.7f, 0.76f,    0.67f, 0.71f,
        0.64f, 0.67f,   0.59f, 0.63f,
        0.55f, 0.59f,   0.46f, 0.55f,
        0.37f, 0.51f
    ))
)

fun main() {
    val ttfInput = DataInputStream(TrueTypeFont::class.java.classLoader.getResourceAsStream("graviks2d/fonts/MainFont.ttf")!!)
    val ttfArray = ttfInput.readAllBytes()
    val ttfBuffer = BufferUtils.createByteBuffer(ttfArray.size)
    ttfBuffer.put(0, ttfArray)

    val fontInfo = STBTTFontinfo.create()
    if (!stbtt_InitFont(fontInfo, ttfBuffer)) {
        throw RuntimeException("Uh ooh")
    }

    val (ascent, descent, lineGap) = stackPush().use { stack ->
        val pAscent = stack.callocInt(1)
        val pDescent = stack.callocInt(1)
        val pLineGap = stack.callocInt(1)

        stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)

        Triple(pAscent[0], pDescent[0], pLineGap[0])
    }

    println("Ascent is $ascent and descent is $descent and lineGap is $lineGap")

    var width = 1
    val height = 1 + ascent - descent

    var numVertices = 0

    val charToDraw = "b".codePointAt(0)
    val shapeToDraw = stbtt_GetCodepointShape(fontInfo, charToDraw)!!
    for (ttfVertex in shapeToDraw) {
        width = max(width, ttfVertex.x() + 1)
        if (ttfVertex.type() == STBTT_vline) {
            numVertices += 3
        }
        if (ttfVertex.type() == STBTT_vcurve) {
            numVertices += 6
            width = max(width, ttfVertex.cx() + 1)
        }
    }

//    val testImage = BufferedImage(width, height, TYPE_INT_ARGB)
//    val graphics = testImage.createGraphics()
//    graphics.color = java.awt.Color.BLUE
//    graphics.stroke = BasicStroke(10f)
//
//    var prevX = 0.toShort()
//    var prevY = 0.toShort()
//
//    for (ttfVertex in shapeToDraw) {
//        ttfVertex.run {
//            val type = type()
//
//            if (type == STBTT_vmove) {
//
//            } else if (type == STBTT_vline) {
//                graphics.drawLine(prevX.toInt(), prevY.toInt() - descent, x().toInt(), y().toInt() - descent)
//            } else if (type == STBTT_vcurve) {
//                graphics.drawLine(prevX.toInt(), prevY.toInt() - descent, x().toInt(), y().toInt() - descent)
//            } else {
//                throw UnsupportedOperationException("No cubics please")
//            }
//            prevX = x()
//            prevY = y()
//        }
//        STBTT_vmove
//    }

    ttfInput.close()

//    graphics.dispose()
//    ImageIO.write(testImage, "PNG", File("test.png"))

    fun transformX(x: Float) = (x * 1000f).toInt()
    fun transformY(y: Float) = ((1 - y) * 1000f).toInt()

    fun transformInvertX(x: Int) = x * 0.001f
    fun transformInvertY(y: Int) = 1f - y * 0.001f

//    for (curve in shapeE.curves) {
//        graphics.color = java.awt.Color.BLUE
//        for (segment in curve) {
//            graphics.drawLine(transformX(segment.startPoint.x), transformY(segment.startPoint.y), transformX(segment.endPoint.x), transformY(segment.endPoint.y))
//        }
//        for (segment in curve) {
//            val radius = 8
//            graphics.color = java.awt.Color.BLACK
//            fun drawPoint(x: Float, y: Float) {
//                graphics.fillOval(transformX(x) - radius / 2, transformY(y) - radius / 2, radius, radius)
//            }
//            drawPoint(segment.startPoint.x, segment.startPoint.y)
//            drawPoint(segment.endPoint.x, segment.endPoint.y)
//            graphics.color = java.awt.Color.RED
//            drawPoint(segment.controlPoint.x, segment.controlPoint.y)
//        }
//    }


//
//    fun triangleArea(p1: Point, p2: Point, p3: Point): Float {
//        val dx2 = p2.x - p1.x
//        val dy2 = p2.y - p1.y
//        val dx3 = p3.x - p1.x
//        val dy3 = p3.y - p1.y
//        return (dx2 * dy3 - dy2 * dx3).absoluteValue / 2f
//    }
//
//    fun isPointInsideTriangle(candidate: Point, t1: Point, t2: Point, t3: Point): Boolean {
//        val triangleArea = triangleArea(t1, t2, t3)
//        val candidateArea = triangleArea(candidate, t1, t2) + triangleArea(candidate, t2, t3) + triangleArea(candidate, t3, t1)
//        return (triangleArea - candidateArea).absoluteValue < 0.0001f
//    }
//
//    fun shouldFlipPoint(candidate: Point, segment: CurveSegment): Boolean {
//        if (isPointInsideTriangle(candidate, segment.startPoint, segment.controlPoint, segment.endPoint)) {
//            val totalArea = triangleArea(segment.startPoint, segment.controlPoint, segment.endPoint)
//
//            // Avoid division by ~0
//            if (totalArea < 0.00001f) {
//                return false
//            }
//
//            // Barycentric coordinates
//            val bcStart = triangleArea(candidate, segment.controlPoint, segment.endPoint) / totalArea
//            val bcControl = triangleArea(candidate, segment.startPoint, segment.endPoint) / totalArea
//            val bcEnd = triangleArea(candidate, segment.startPoint, segment.controlPoint) / totalArea
//
//            val s = bcControl
//            val t = max(bcStart, bcEnd)
//
//            return (s / 2f + t).pow(2) < t
//        } else {
//            return false
//        }
//    }
//
//    val anchorPoint = Point(0f, 0f)
//
//    for (intX in 0 until width) {
//        for (intY in 0 until height) {
//            val x = transformInvertX(intX)
//            val y = transformInvertY(intY)
//
//            var numIntersections = 0
//            for (curve in shapeE.curves) {
//                for (segment in curve) {
//                    if (isPointInsideTriangle(Point(x, y), anchorPoint, segment.startPoint, segment.endPoint)) {
//                        numIntersections += 1
//                    }
//                    if (shouldFlipPoint(Point(x, y), segment)) {
//                        numIntersections += 1
//                    }
//                }
//            }
//
//            if (numIntersections % 2 == 0) {
//                testImage.setRGB(intX, intY, java.awt.Color.BLACK.rgb)
//            } else {
//                testImage.setRGB(intX, intY, java.awt.Color.BLUE.rgb)
//            }
//        }
//    }
//
//    graphics.dispose()
//    ImageIO.write(testImage, "PNG", File("test.png"))

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

        val ciTestBuffer = VkBufferCreateInfo.calloc(stack)
        ciTestBuffer.`sType$Default`()
        ciTestBuffer.size((width * height * 1).toLong())
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
        val testHostBuffer = memByteBuffer(testAllocationInfo.pMappedData(), width * height * 1)

        val ciImage = VkImageCreateInfo.calloc(stack)
        ciImage.`sType$Default`()
        ciImage.imageType(VK_IMAGE_TYPE_2D)
        ciImage.format(VK_FORMAT_R8_UNORM)
        ciImage.extent().set(width, height, 1)
        ciImage.mipLevels(1)
        ciImage.arrayLayers(1)
        ciImage.samples(VK_SAMPLE_COUNT_1_BIT)
        ciImage.tiling(VK_IMAGE_TILING_OPTIMAL)
        ciImage.usage(
            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
                    or VK_IMAGE_USAGE_SAMPLED_BIT
                    or VK_IMAGE_USAGE_TRANSFER_SRC_BIT
        )
        ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

        val ciImageAllocation = VmaAllocationCreateInfo.calloc(stack)
        ciImageAllocation.usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE)

        val pImage = stack.callocLong(1)
        val pImageAllocation = stack.callocPointer(1)
        assertSuccess(
            vmaCreateImage(vmaAllocator, ciImage, ciImageAllocation, pImage, pImageAllocation, null),
            "vmaCreateImage"
        )
        val image = pImage[0]
        val imageAllocation = pImageAllocation[0]

        val ciImageView = VkImageViewCreateInfo.calloc(stack)
        ciImageView.`sType$Default`()
        ciImageView.image(image)
        ciImageView.viewType(VK_IMAGE_VIEW_TYPE_2D)
        ciImageView.format(VK_FORMAT_R8_UNORM)
        ciImageView.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
        ciImageView.subresourceRange {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.baseMipLevel(0)
            it.levelCount(1)
            it.baseArrayLayer(0)
            it.layerCount(1)
        }

        val pImageView = stack.callocLong(1)
        assertSuccess(
            vkCreateImageView(vkDevice, ciImageView, null, pImageView),
            "vkCreateImageView"
        )
        val imageView = pImageView[0]

        val ciVertexBuffer = VkBufferCreateInfo.calloc(stack)
        ciVertexBuffer.`sType$Default`()
        ciVertexBuffer.size((numVertices * TextVertex.BYTE_SIZE).toLong())
        ciVertexBuffer.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        ciVertexBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

        val ciVertexAllocation = VmaAllocationCreateInfo.calloc(stack)
        ciVertexAllocation.flags(
            VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or
                    VMA_ALLOCATION_CREATE_MAPPED_BIT
        )
        ciVertexAllocation.usage(VMA_MEMORY_USAGE_AUTO)

        val pVertexBuffer = stack.callocLong(1)
        val pVertexAllocation = stack.callocPointer(1)
        val vertexInfo = VmaAllocationInfo.calloc(stack)

        assertSuccess(
            vmaCreateBuffer(vmaAllocator, ciVertexBuffer, ciVertexAllocation, pVertexBuffer, pVertexAllocation, vertexInfo),
            "vmaCreateBuffer"
        )
        val vertexBuffer = pVertexBuffer[0]
        val vertexAllocation = pVertexAllocation[0]

        val hostVertexByteBuffer = memByteBuffer(vertexInfo.pMappedData(), ciVertexBuffer.size().toInt())
        val hostVertexBuffer = TextVertexBuffer.createAtBuffer(hostVertexByteBuffer, numVertices)

        var vertexIndex = 0
        var prevX = 0f
        var prevY = 0f

        for (ttfVertex in shapeToDraw) {
            ttfVertex.run {
                fun transformPoint(x: Short, y: Short) = Pair(
                    (x.toFloat() + 0.5f) / width.toFloat(),
                    (y.toFloat() + 0.5f - descent.toFloat()) / height.toFloat()
                )
                val (currentX, currentY) = transformPoint(x(), y())

                if (type() == STBTT_vline || type() == STBTT_vcurve) {
                    hostVertexBuffer[vertexIndex].run {
                        this.x = prevX
                        this.y = prevY
                        this.operation = OPERATION_INCREMENT
                    }
                    hostVertexBuffer[vertexIndex + 1].run {
                        this.x = currentX
                        this.y = currentY
                        this.operation = OPERATION_INCREMENT
                    }
                    hostVertexBuffer[vertexIndex + 2].run {
                        this.x = 0f
                        this.y = 0f
                        this.operation = OPERATION_INCREMENT
                    }
                    vertexIndex += 3
                }
                if (type() == STBTT_vcurve) {
                    val (controlX, controlY) = transformPoint(cx(), cy())
                    hostVertexBuffer[vertexIndex].run {
                        this.x = prevX
                        this.y = prevY
                        this.operation = OPERATION_CORRECT_START
                    }
                    hostVertexBuffer[vertexIndex + 1].run {
                        this.x = controlX
                        this.y = controlY
                        this.operation = OPERATION_CORRECT_CONTROL
                    }
                    hostVertexBuffer[vertexIndex + 2].run {
                        this.x = currentX
                        this.y = currentY
                        this.operation = OPERATION_CORRECT_END
                    }
                    vertexIndex += 3
                }
                prevX = currentX
                prevY = currentY
            }
        }
//        for (segment in shapeE.curveSegments) {
//            // Increment
//            hostVertexBuffer[vertexIndex].run {
//                this.x = segment.startPoint.x
//                this.y = segment.startPoint.y
//                this.operation = OPERATION_INCREMENT
//            }
//            hostVertexBuffer[vertexIndex + 1].run {
//                this.x = segment.endPoint.x
//                this.y = segment.endPoint.y
//                this.operation = OPERATION_INCREMENT
//            }
//            hostVertexBuffer[vertexIndex + 2].run {
//                this.x = 0f
//                this.y = 0f
//                this.operation = OPERATION_INCREMENT
//            }
//            hostVertexBuffer[vertexIndex + 3].run {
//                this.x = segment.startPoint.x
//                this.y = segment.startPoint.y
//                this.operation = OPERATION_CORRECT_START
//            }
//            hostVertexBuffer[vertexIndex + 4].run {
//                this.x = segment.controlPoint.x
//                this.y = segment.controlPoint.y
//                this.operation = OPERATION_CORRECT_CONTROL
//            }
//            hostVertexBuffer[vertexIndex + 5].run {
//                this.x = segment.endPoint.x
//                this.y = segment.endPoint.y
//                this.operation = OPERATION_CORRECT_END
//            }
//            vertexIndex += 6
//        }

        val textPipeline = TextPipeline(vkDevice)

        val ciFramebuffer = VkFramebufferCreateInfo.calloc(stack)
        ciFramebuffer.`sType$Default`()
        ciFramebuffer.renderPass(textPipeline.vkRenderPass)
        ciFramebuffer.attachmentCount(1)
        ciFramebuffer.pAttachments(stack.longs(imageView))
        ciFramebuffer.width(width)
        ciFramebuffer.height(height)
        ciFramebuffer.layers(1)

        val pFramebuffer = stack.callocLong(1)
        assertSuccess(
            vkCreateFramebuffer(vkDevice, ciFramebuffer, null, pFramebuffer),
            "vkCreateFramebuffer"
        )
        val framebuffer = pFramebuffer[0]

        val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
        ciCommandPool.`sType$Default`()
        ciCommandPool.queueFamilyIndex(graphicsQueueIndex)

        val pCommandPool = stack.callocLong(1)
        assertSuccess(
            vkCreateCommandPool(vkDevice, ciCommandPool, null, pCommandPool),
            "vkCreateCommandPool"
        )
        val commandPool = pCommandPool[0]

        val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
        aiCommandBuffer.`sType$Default`()
        aiCommandBuffer.commandPool(commandPool)
        aiCommandBuffer.commandBufferCount(1)
        aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)

        val pCommandBuffer = stack.callocPointer(1)
        assertSuccess(
            vkAllocateCommandBuffers(vkDevice, aiCommandBuffer, pCommandBuffer),
            "vkAllocateCommandBuffers"
        )
        val commandBuffer = VkCommandBuffer(pCommandBuffer[0], vkDevice)

        val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
        biCommandBuffer.`sType$Default`()

        assertSuccess(
            vkBeginCommandBuffer(commandBuffer, biCommandBuffer),
            "vkBeginCommandBuffer"
        )

        val clearValues = VkClearValue.calloc(1, stack)
        val clearValue = clearValues[0]
        clearValue.color().int32(0, 0)

        val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
        biRenderPass.`sType$Default`()
        biRenderPass.renderPass(textPipeline.vkRenderPass)
        biRenderPass.framebuffer(framebuffer)
        biRenderPass.renderArea {
            it.offset().set(0, 0)
            it.extent().set(width, height)
        }
        biRenderPass.clearValueCount(1)
        biRenderPass.pClearValues(clearValues)

        val viewports = VkViewport.calloc(1, stack)
        val viewport = viewports[0]
        viewport.x(0f)
        viewport.y(0f)
        viewport.width(width.toFloat())
        viewport.height(height.toFloat())
        viewport.minDepth(0f)
        viewport.minDepth(1f)

        val scissors = VkRect2D.calloc(1, stack)
        val scissor = scissors[0]
        scissor.offset().set(0, 0)
        scissor.extent().set(width, height)

        vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, textPipeline.vkPipeline)
        vkCmdSetViewport(commandBuffer, 0, viewports)
        vkCmdSetScissor(commandBuffer, 0, scissors)
        vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(vertexBuffer), stack.longs(0))
        vkCmdDraw(commandBuffer, numVertices, 1, 0, 0)
        vkCmdEndRenderPass(commandBuffer)

        // TODO Insert pipeline barrier

        val copyRanges = VkBufferImageCopy.calloc(1, stack)
        val copyRange = copyRanges[0]
        copyRange.bufferOffset(0)
        copyRange.bufferRowLength(width)
        copyRange.bufferImageHeight(height)
        copyRange.imageSubresource {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.mipLevel(0)
            it.baseArrayLayer(0)
            it.layerCount(1)
        }
        copyRange.imageOffset().set(0, 0, 0)
        copyRange.imageExtent().set(width, height, 1)

        vkCmdCopyImageToBuffer(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, testBuffer, copyRanges)
        // TODO Insert pipeline barrier

        assertSuccess(
            vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer"
        )

        val ciFence = VkFenceCreateInfo.calloc(stack)
        ciFence.`sType$Default`()

        val pFence = stack.callocLong(1)
        assertSuccess(
            vkCreateFence(vkDevice, ciFence, null, pFence),
            "vkCreateFence"
        )
        val fence = pFence[0]

        val pSubmitInfo = VkSubmitInfo.calloc(1, stack)
        val submitInfo = pSubmitInfo[0]
        submitInfo.`sType$Default`()
        submitInfo.pCommandBuffers(stack.pointers(commandBuffer.address()))

        assertSuccess(
            vkQueueSubmit(queue, pSubmitInfo, fence), "vkQueueSubmit"
        )

        assertSuccess(
            vkWaitForFences(vkDevice, pFence, true, -1),
            "vkWaitForFences"
        )

        val testBufferedImage = BufferedImage(width, height, TYPE_INT_ARGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val rawValue = testHostBuffer[x + y * width]
                val color = java.awt.Color(200 * (rawValue.toUByte().toInt() % 2), 0, 0)
                testBufferedImage.setRGB(x, y, color.rgb)
            }
        }
        ImageIO.write(testBufferedImage, "PNG", File("testText.png"))

        vkDestroyFence(vkDevice, fence, null)
        vkDestroyCommandPool(vkDevice, commandPool, null)
        vkDestroyFramebuffer(vkDevice, framebuffer, null)
        textPipeline.destroy()
        vmaDestroyBuffer(vmaAllocator, vertexBuffer, vertexAllocation)
        vkDestroyImageView(vkDevice, imageView, null)
        vmaDestroyImage(vmaAllocator, image, imageAllocation)
        vmaDestroyBuffer(vmaAllocator, testBuffer, testAllocation)
        vmaDestroyAllocator(vmaAllocator)
        vkDestroyDevice(vkDevice, null)
        vkDestroyInstance(vkInstance, null)
    }
}
