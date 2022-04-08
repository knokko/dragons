package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.pipeline.*
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_BOTTOM_LEFT
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_TOP_LEFT
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_TOP_RIGHT
import graviks2d.pipeline.OP_CODE_FILL_RECT
import graviks2d.resource.image.BorrowedImage
import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.TextCacheArea
import graviks2d.resource.text.TextShapeCache
import graviks2d.util.Color
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException
import kotlin.math.roundToInt

class GraviksContext(
    val instance: GraviksInstance,
    val width: Int,
    val height: Int,
    val translucentPolicy: TranslucentPolicy,

    /**
     * The `depthPolicy` determines what happens if you draw multiple objects in the same area. In the default
     * `Automatic` policy, more recently drawn objects will be drawn in front of older objects. (This is almost always
     * what you want.)
     *
     * The `Manual` policy allows you to set the `depth` of each object manually. (Objects with higher `depth` will be
     * drawn in front of objects with a lower `depth`.) This allows you to control exactly which objects are drawn in
     * the front and which objects are drawn in the back, but is more complicated.
     */
    val depthPolicy: DepthPolicy = DepthPolicy.Automatic,

    /**
     * When creating a new `GraviksContext`, its target image will be filled with the `initialBackgroundColor`. You
     * can see this background color if you view the target image before doing any drawing operations or color clears.
     */
    initialBackgroundColor: Color = Color.rgbInt(100, 100, 100),

    /**
     * The maximum value of the *depth counter*. Most drawing methods of this class (like `fillRect`) increase the
     * *depth counter* by 1. When the *depth counter* reaches the `maxDepth`, the context will force a flush, reset the
     * *depth counter*, and clear the depth buffer.
     *
     * ## Value considerations
     * Forcing a flush and clearing the depth buffer takes time, so this should not be done too often. Using a higher
     * `maxDepth` will result in lesser flushes and depth clears, but is also more likely to cause
     * 'depth-buffer fighting':
     *
     * ## Depth buffer motivation
     * When the user calls 2 drawing methods of this class that affect the same region, he would want the second draw
     * to overwrite (a part of) the first draw. However, due to performance reasons, Graviks2D performs most drawing
     * work *in parallel*. If no measures were taken by Graviks2D, there would be no guarantee that the second draw
     * really draws in front of the first: their order could just as well be exchanged or even mixed. To avoid this
     * problem, Graviks2D will use the *depth test* and give later drawing operations a depth that is closer to the
     * 'camera'.
     *
     * ## Depth buffer problems
     * Unfortunately, there are only finitely many distinct floating point values between 0.0 and 1.0. (Vulkan only
     * accepts depth values between 0 and 1.) When there are more drawing operations than distinct floating point
     * values, it is not possible to give every drawing operation their own depth value, which will cause some
     * drawing operations to have the same depth. When the drawing area of such drawing operations overlap, depth-buffer
     * fighting can occur.
     *
     * ## The default value
     * My experiments showed that the 'critical value' of the `maxDepth` seems to be between 8million and 9million
     * (when using 24 depth bits in the Vulkan depth format, which is the minimum number of depth bits Graviks allows).
     * To be safe, I use 1million as the default value. This means that a flush and depth clear will be needed every
     * million drawing operations. The overhead of 1 flush + depth clear should not be significant compared to the time
     * it takes to perform a million drawing operations, so I expect almost no performance consequences. Everyone who
     * disagrees is of course free to choose a different value.
     */
    val maxDepth: Int = 1_000_000,

    /**
     * The maximum number of **vertices** that can fit in the vertex buffer
     */
    vertexBufferSize: Int = 5000,

    /**
     * The maximum number of **int**s that can fit in the operation buffer
     */
    operationBufferSize: Int = 25000
) {

    internal val targetImages = ContextTargetImages(this)
    internal val buffers = ContextBuffers(this, vertexBufferSize, operationBufferSize)

    private var currentDepth = -1

    private var currentVertexIndex = -1
    private var currentOperationIndex = -1

    private val commands: ContextCommands
    private val currentImages = mutableMapOf<BorrowedImage, Int>()
    internal val textShapeCache = TextShapeCache(
        this // TODO Configure the other parameters
    )
    internal val descriptors = ContextDescriptors(
        this.instance, this.buffers.operationVkBuffer, this.textShapeCache.textAtlasView
    )

    private val queuedDrawCommands: MutableList<DrawCommand> = mutableListOf()
    private var clearDepthBeforeNextDrawCommand = false

    init {
        if (translucentPolicy != TranslucentPolicy.Manual) {
            throw UnsupportedOperationException("This TranslucentPolicy is not yet implemented")
        }

        this.commands = ContextCommands(this)
        this.clearDepthBeforeNextDrawCommand = true
        this.pushRect(0f, 0f, 1f, 1f, vertexIndex = 0, depth = 0, operationIndex = 0)
        this.buffers.operationCpuBuffer.run {
            this.put(0, 1) // 1 is the op code for fill rect
            this.put(1, initialBackgroundColor.rawValue)
        }
        currentVertexIndex = 6 // The first 6 vertices are used for the color clear
        currentOperationIndex = 2 // The first 2 operation values are used for the color clear
        if (depthPolicy == DepthPolicy.Automatic) {
            currentDepth = 1
        }
    }

    private fun claimNextVertexIndex(numVertices: Int): Int {
        if (numVertices > this.buffers.vertexBufferSize) {
            throw IllegalArgumentException(
                "Too many vertices ($numVertices): at most ${this.buffers.vertexBufferSize} are allowed. " +
                        "Pass a larger value for vertexBufferSize to the constructor of this class to overwrite it."
            )
        }
        if (this.currentVertexIndex + numVertices > this.buffers.vertexBufferSize) {
            this.hardFlush()
            // hardFlush() should set currentVertexIndex to 0
        }
        val result = this.currentVertexIndex
        this.currentVertexIndex += numVertices
        return result
    }

    private fun claimNextOperationIndex(numIntValues: Int): Int {
        if (numIntValues > this.buffers.operationBufferSize) {
            throw IllegalArgumentException(
                "Too many operation integers ($numIntValues): at most ${this.buffers.operationBufferSize} are allowed. " +
                        "Pass a larger value for operationBufferSize to the constructor of this class to overwrite it."
            )
        }
        if (this.currentOperationIndex + numIntValues > this.buffers.operationBufferSize) {
            this.hardFlush()
            // hardFlush() should set currentOperationIndex to 0
        }
        val result = this.currentOperationIndex
        this.currentOperationIndex += numIntValues
        return result
    }

    private fun claimNextDepth(): Int {
        var depth = this.currentDepth
        if (this.depthPolicy == DepthPolicy.Automatic) {
            if (depth > this.maxDepth) {
                this.softFlush()
                this.clearDepthBeforeNextDrawCommand = true
                this.currentDepth = 1
                depth = 1
            }
            this.currentDepth += 1
        }

        return depth
    }

    // TODO Allow manual depth clear and test it

    private fun softFlush() {
        val firstVertexIndex = if (this.queuedDrawCommands.isEmpty()) { 0 } else {
            this.queuedDrawCommands[0].vertexIndex + this.queuedDrawCommands[0].numVertices
        }
        if (firstVertexIndex != this.currentVertexIndex) {
            this.queuedDrawCommands.add(
                DrawCommand(
                    preDepthClear = this.clearDepthBeforeNextDrawCommand,
                    vertexIndex = firstVertexIndex,
                    numVertices = this.currentVertexIndex - firstVertexIndex
                )
            )
            this.clearDepthBeforeNextDrawCommand = false
        }
        this.currentVertexIndex = firstVertexIndex
    }

    private fun hardFlush() {
        if (currentVertexIndex < 0) {
            throw IllegalStateException("Current vertex index ($currentVertexIndex) must be non-negative")
        }
        if (currentOperationIndex < 0) {
            throw IllegalStateException("Current operation index ($currentOperationIndex) must be non-negative")
        }

        this.softFlush()

        // If no draw commands are queued, we can skip it and spare the synchronization overhead
        if (queuedDrawCommands.isNotEmpty()) {
            runBlocking {
                val imageViewsArray = Array<Long>(currentImages.size) { 0 }
                for ((borrowedImage, index) in currentImages) {
                    imageViewsArray[index] = borrowedImage.imagePair.await().vkImageView
                }
                descriptors.updateDescriptors(imageViewsArray)
            }
            commands.draw(queuedDrawCommands)
            for (borrowedImage in currentImages.keys) {
                instance.imageCache.returnImage(borrowedImage)
            }
            currentImages.clear()
            queuedDrawCommands.clear()
        }

        currentVertexIndex = 0
        currentOperationIndex = 0
        textShapeCache.clear()
    }

    private fun pushRect(
        x1: Float, y1: Float, x2: Float, y2: Float, vertexIndex: Int, depth: Int, operationIndex: Int
    ) {
        this.pushRect(x1, y1, x2, y2, vertexIndex, depth, operationIndex, operationIndex, operationIndex, operationIndex)
    }

    private fun pushRect(
        x1: Float, y1: Float, x2: Float, y2: Float, vertexIndex: Int, depth: Int,
        operationIndex1: Int, operationIndex2: Int, operationIndex3: Int, operationIndex4: Int
    ) {
        this.buffers.vertexCpuBuffer.run {
            this[vertexIndex].x = x1
            this[vertexIndex].y = y1
            this[vertexIndex].depth = depth
            this[vertexIndex].operationIndex = operationIndex1

            this[vertexIndex + 1].x = x2
            this[vertexIndex + 1].y = y1
            this[vertexIndex + 1].depth = depth
            this[vertexIndex + 1].operationIndex = operationIndex2

            this[vertexIndex + 2].x = x2
            this[vertexIndex + 2].y = y2
            this[vertexIndex + 2].depth = depth
            this[vertexIndex + 2].operationIndex = operationIndex3

            this[vertexIndex + 3].x = x2
            this[vertexIndex + 3].y = y2
            this[vertexIndex + 3].depth = depth
            this[vertexIndex + 3].operationIndex = operationIndex3

            this[vertexIndex + 4].x = x1
            this[vertexIndex + 4].y = y2
            this[vertexIndex + 4].depth = depth
            this[vertexIndex + 4].operationIndex = operationIndex4

            this[vertexIndex + 5].x = x1
            this[vertexIndex + 5].y = y1
            this[vertexIndex + 5].depth = depth
            this[vertexIndex + 5].operationIndex = operationIndex1
        }
    }

    fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {

        val operationIndex = this.claimNextOperationIndex(2)
        val depth = this.claimNextDepth()

        // Note: claimNextVertexIndex MUST be called as last to avoid weird situations when a flush() occurs BEFORE
        // the vertices are actually populated.
        val vertexIndex = this.claimNextVertexIndex(6)

        this.pushRect(x1, y1, x2, y2, vertexIndex = vertexIndex, depth = depth, operationIndex = operationIndex)

        this.buffers.operationCpuBuffer.run {
            this.put(operationIndex, OP_CODE_FILL_RECT)
            this.put(operationIndex + 1, color.rawValue)
        }
    }

    fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        val borrowedImage = this.instance.imageCache.borrowImage(image)
        var imageIndex = this.currentImages[borrowedImage]
        if (imageIndex == null) {
            if (this.currentImages.size >= this.instance.maxNumDescriptorImages) {
                this.hardFlush()
            }
            imageIndex = this.currentImages.size
            this.currentImages[borrowedImage] = imageIndex
        }

        val operationIndex = this.claimNextOperationIndex(5)
        val depth = this.claimNextDepth()
        val vertexIndex = this.claimNextVertexIndex(6)

        this.pushRect(
            xLeft, yBottom, xRight, yTop, vertexIndex = vertexIndex, depth = depth,
            operationIndex1 = operationIndex, operationIndex2 = operationIndex + 2,
            operationIndex3 = operationIndex + 3, operationIndex4 = operationIndex + 4
        )

        this.buffers.operationCpuBuffer.run {
            this.put(operationIndex, OP_CODE_DRAW_IMAGE_BOTTOM_LEFT)
            this.put(operationIndex + 1, imageIndex)
            this.put(operationIndex + 2, OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT)
            this.put(operationIndex + 3, OP_CODE_DRAW_IMAGE_TOP_RIGHT)
            this.put(operationIndex + 4, OP_CODE_DRAW_IMAGE_TOP_LEFT)
        }
    }

    fun drawString(
        minX: Float, yBottom: Float, maxX: Float, yTop: Float,
        string: String, textColor: Color, backgroundColor: Color, // TODO Allow choosing a font
    // TODO Allow choosing horizontal text alignment and respect maxX
    ) {
        val font = this.instance.defaultFont

        // Good text rendering requires exact placement on pixels
        val pixelMinY = (yBottom * this.height.toFloat()).roundToInt()
        val pixelBoundY = (yTop * this.height.toFloat()).roundToInt()
        val finalMinY = pixelMinY.toFloat() / this.height.toFloat()
        val finalMaxY = pixelBoundY.toFloat() / this.height.toFloat()

        var currentPixelMinX = (minX * this.width.toFloat()).roundToInt()

        for (codepoint in string.codePoints()) {
            val glyphShape = font.getGlyphShape(codepoint)

            val charHeight = pixelBoundY - pixelMinY
            val shapeAspectRatio = glyphShape.advanceWidth.toFloat() / (font.ascent - font.descent).toFloat()
            val charWidth = (charHeight.toFloat() * shapeAspectRatio).roundToInt()

            val currentPixelBoundX = currentPixelMinX + charWidth

            val currentDrawMinX = currentPixelMinX.toFloat() / this.width.toFloat()
            val currentDrawMaxX = currentPixelBoundX.toFloat() / this.width.toFloat()

            if (glyphShape.ttfVertices != null) {

                var textCacheArea = this.textShapeCache.prepareCharacter(
                    codepoint, font.ascent, font.descent, glyphShape, charWidth, charHeight
                )
                if (textCacheArea == null) {
                    this.hardFlush()
                    textCacheArea = this.textShapeCache.prepareCharacter(
                        codepoint, font.ascent, font.descent, glyphShape, charWidth, charHeight
                    )
                    if (textCacheArea == null) {
                        throw IllegalArgumentException("Can't draw character with codepoint $codepoint, not even after a hard flush")
                    }
                }

                val operationSize = 5
                val operationIndex = this.claimNextOperationIndex(4 * operationSize)
                val depth = this.claimNextDepth()
                val vertexIndex = this.claimNextVertexIndex(6)

                this.pushRect(
                    currentDrawMinX, finalMinY, currentDrawMaxX, finalMaxY,
                    vertexIndex, depth,
                    operationIndex, operationIndex + operationSize,
                    operationIndex + 2 * operationSize,
                    operationIndex + 3 * operationSize
                )

                fun encodeFloat(value: Float) = (1_000_000.toFloat() * value).roundToInt()

                this.buffers.operationCpuBuffer.run {
                    for ((startOperationIndex, texX, texY) in arrayOf(
                        Triple(operationIndex, textCacheArea.minX, textCacheArea.maxY),
                        Triple(operationIndex + operationSize, textCacheArea.maxX, textCacheArea.maxY),
                        Triple(operationIndex + 2 * operationSize, textCacheArea.maxX, textCacheArea.minY),
                        Triple(operationIndex + 3 * operationSize, textCacheArea.minX, textCacheArea.minY)
                    )) {
                        this.put(startOperationIndex, OP_CODE_DRAW_TEXT)
                        this.put(startOperationIndex + 1, encodeFloat(texX))
                        this.put(startOperationIndex + 2, encodeFloat(texY))
                        this.put(startOperationIndex + 3, textColor.rawValue)
                        this.put(startOperationIndex + 4, backgroundColor.rawValue)
                    }
                }
            }
            currentPixelMinX = currentPixelBoundX
        }
    }

    fun setManualDepth(newDepth: Int) {
        if (depthPolicy != DepthPolicy.Manual) {
            throw UnsupportedOperationException("setManualDepth is only allowed when depthMode is Manual")
        }
        if (newDepth > maxDepth) {
            throw IllegalArgumentException("Can't set depth to $newDepth because the maxDepth is $maxDepth.")
        }
        currentDepth = newDepth
    }

    fun copyColorImageTo(destImage: Long?, destBuffer: Long?) {
        hardFlush()
        commands.copyColorImageTo(destImage = destImage, destBuffer = destBuffer)
    }

    fun destroy() {
        commands.destroy()
        textShapeCache.destroy()
        targetImages.destroy()
        descriptors.destroy()
        buffers.destroy()
    }
}
