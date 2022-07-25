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
import graviks2d.resource.text.*
import graviks2d.resource.text.TextShapeCache
import graviks2d.resource.text.placeText
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException
import java.lang.Integer.min
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
    operationBufferSize: Int = 25000,

    /**
     * The width of the text cache, in **pixels**
     */
    textCacheWidth: Int = width,

    /**
     * The height of the text cache, in **pixels**
     */
    textCacheHeight: Int = height,

    /**
     * The maximum number of **vertices** that can fit in the text vertex buffer
     */
    textVertexBufferSize: Int = 60_000,

    /**
     * The maximum number of **rectangles** that can fit in the rectangle packing buffer of the text renderer. This is
     * also an upperbound on the number of characters that can be drawn in parallel.
     */
    textRectanglePackingBufferSize: Int = 5_000,

    /**
     * The number of **nodes** of the rectangle packing context of the text renderer.
     */
    textRectanglePackingNodeBufferSize: Int = 2 * width
): GraviksTarget {

    internal val targetImages = ContextTargetImages(this)
    internal val buffers = ContextBuffers(this, vertexBufferSize, operationBufferSize)

    private var currentDepth = -1

    private var currentVertexIndex = -1
    private var currentOperationIndex = -1

    private val commands: ContextCommands
    private val currentImages = mutableMapOf<BorrowedImage, Int>()
    internal val textShapeCache = TextShapeCache(
        context = this, width = textCacheWidth, height = textCacheHeight,
        vertexBufferSize = textVertexBufferSize, rectanglePackingBufferSize = textRectanglePackingBufferSize,
        rectanglePackingNodeBufferSize = textRectanglePackingNodeBufferSize
    )
    internal val descriptors = ContextDescriptors(
        this.instance, this.buffers.operationVkBuffer, this.textShapeCache.textOddAtlasView
    )

    private val queuedDrawCommands: MutableList<DrawCommand> = mutableListOf()
    private var clearDepthBeforeNextDrawCommand = false

    init {
        // TODO Add support for more translucent policies
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

    private fun encodeFloat(value: Float) = (1_000_000.toFloat() * value).roundToInt()

    private fun claimSpace(numVertices: Int, numOperationValues: Int): ContextSpaceClaim {
        var (operationIndex, didOperationFlush) = this.claimNextOperationIndex(numOperationValues)
        var depth = this.claimNextDepth()

        // Note: claimNextVertexIndex MUST be called as last to avoid weird situations when a flush() occurs BEFORE
        // the vertices are actually populated.
        val (vertexIndex, didVertexFlush) = this.claimNextVertexIndex(numVertices)

        // If a vertex flush occurred, the operation index and depth will be invalid and need to be recomputed
        if (didVertexFlush) {
            // Note: claiming this operation index can NOT cause another hard flush because the operation buffer is empty
            operationIndex = this.claimNextOperationIndex(numOperationValues).first
            depth = this.claimNextDepth()
        }

        return ContextSpaceClaim(
            vertexIndex = vertexIndex,
            operationIndex = operationIndex,
            depth = depth,
            didHardFlush = didOperationFlush || didVertexFlush
        )
    }

    private fun claimNextVertexIndex(numVertices: Int): Pair<Int, Boolean> {
        var didHardFlush = false
        if (numVertices > this.buffers.vertexBufferSize) {
            throw IllegalArgumentException(
                "Too many vertices ($numVertices): at most ${this.buffers.vertexBufferSize} are allowed. " +
                        "Pass a larger value for vertexBufferSize to the constructor of this class to overwrite it."
            )
        }
        if (this.currentVertexIndex + numVertices > this.buffers.vertexBufferSize) {
            this.hardFlush()
            didHardFlush = true
            // hardFlush() should set currentVertexIndex to 0
        }
        val result = this.currentVertexIndex
        this.currentVertexIndex += numVertices
        return Pair(result, didHardFlush)
    }

    private fun claimNextOperationIndex(numIntValues: Int): Pair<Int, Boolean> {
        var didHardFlush = false
        if (numIntValues > this.buffers.operationBufferSize) {
            throw IllegalArgumentException(
                "Too many operation integers ($numIntValues): at most ${this.buffers.operationBufferSize} are allowed. " +
                        "Pass a larger value for operationBufferSize to the constructor of this class to overwrite it."
            )
        }
        if (this.currentOperationIndex + numIntValues > this.buffers.operationBufferSize) {
            this.hardFlush()
            didHardFlush = true
            // hardFlush() should set currentOperationIndex to 0
        }
        val result = this.currentOperationIndex
        this.currentOperationIndex += numIntValues
        return Pair(result, didHardFlush)
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

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 2)

        this.pushRect(x1, y1, x2, y2, vertexIndex = claimedSpace.vertexIndex, depth = claimedSpace.depth, operationIndex = claimedSpace.operationIndex)

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_FILL_RECT)
            this.put(claimedSpace.operationIndex + 1, color.rawValue)
        }
    }

    override fun drawRoundedRect(
        x1: Float, y1: Float, x2: Float, y2: Float, radiusX: Float, lineWidth: Float, color: Color
    ) {
        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 8)

        this.pushRect(x1, y1, x2, y2, vertexIndex = claimedSpace.vertexIndex, depth = claimedSpace.depth, operationIndex = claimedSpace.operationIndex)

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_DRAW_ROUNDED_RECT)
            this.put(claimedSpace.operationIndex + 1, color.rawValue)
            this.put(claimedSpace.operationIndex + 2, encodeFloat(kotlin.math.min(x1, x2)))
            this.put(claimedSpace.operationIndex + 3, encodeFloat(kotlin.math.min(y1, y2)))
            this.put(claimedSpace.operationIndex + 4, encodeFloat(kotlin.math.max(x1, x2)))
            this.put(claimedSpace.operationIndex + 5, encodeFloat(kotlin.math.max(y1, y2)))
            this.put(claimedSpace.operationIndex + 6, encodeFloat(radiusX))
            this.put(claimedSpace.operationIndex + 7, encodeFloat(lineWidth))
        }
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        val borrowedImage = this.instance.imageCache.borrowImage(image)
        var imageIndex = this.currentImages[borrowedImage]
        if (imageIndex == null) {
            if (this.currentImages.size >= this.instance.maxNumDescriptorImages) {
                this.hardFlush()
            }
            imageIndex = this.currentImages.size
            this.currentImages[borrowedImage] = imageIndex
        }

        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 5)

        this.pushRect(
            xLeft, yBottom, xRight, yTop, vertexIndex = claimedSpace.vertexIndex, depth = claimedSpace.depth,
            operationIndex1 = claimedSpace.operationIndex, operationIndex2 = claimedSpace.operationIndex + 2,
            operationIndex3 = claimedSpace.operationIndex + 3, operationIndex4 = claimedSpace.operationIndex + 4
        )

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_DRAW_IMAGE_BOTTOM_LEFT)
            this.put(claimedSpace.operationIndex + 1, imageIndex)
            this.put(claimedSpace.operationIndex + 2, OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT)
            this.put(claimedSpace.operationIndex + 3, OP_CODE_DRAW_IMAGE_TOP_RIGHT)
            this.put(claimedSpace.operationIndex + 4, OP_CODE_DRAW_IMAGE_TOP_LEFT)
        }
    }

    override fun getImageSize(image: ImageReference): Pair<Int, Int> {
        if (image.customVkImage != null) return Pair(image.customWidth!!, image.customHeight!!)

        val borrow = this.instance.imageCache.borrowImage(image)
        val size = runBlocking {
            val imagePair = borrow.imagePair.await()
            Pair(imagePair.width, imagePair.height)
        }
        this.instance.imageCache.returnImage(borrow)
        return size
    }

    override fun drawString(
        minX: Float, yBottom: Float, maxX: Float, yTop: Float,
        string: String, style: TextStyle, backgroundColor: Color,
    ) {
        val font = this.instance.fontManager.getFont(style.font)
        val placedChars = placeText(minX, yBottom, maxX, yTop, string, style, font, this.width, this.height)
        for (placedChar in placedChars) {
            val glyphShape = font.getGlyphShape(placedChar.codepoint)

            val maxAntiAliasFactor = min(
                this.textShapeCache.width / placedChar.pixelWidth,
                this.textShapeCache.height / placedChar.pixelHeight
            )
            val antiAliasFactor = min(if (placedChar.pixelHeight < 15) {
                4
            } else if (placedChar.pixelHeight < 200) {
                2
            } else {
                1
            }, maxAntiAliasFactor)

            val cachedWidth = placedChar.pixelWidth * antiAliasFactor
            val cachedHeight = placedChar.pixelHeight * antiAliasFactor
            if (glyphShape.ttfVertices != null) {

                var textCacheArea = this.textShapeCache.prepareCharacter(
                    placedChar.codepoint, font.ascent, font.descent,
                    glyphShape, cachedWidth, cachedHeight
                )
                if (textCacheArea == null) {
                    this.hardFlush()
                    textCacheArea = this.textShapeCache.prepareCharacter(
                        placedChar.codepoint, font.ascent, font.descent,
                        glyphShape, cachedWidth, cachedHeight
                    )
                    if (textCacheArea == null) {
                        throw IllegalArgumentException("Can't draw character with codepoint ${placedChar.codepoint}, not even after a hard flush")
                    }
                }

                val strokeDeltaY = (yTop - yBottom) * style.strokeHeightFraction
                val strokeDeltaX = strokeDeltaY * this.textShapeCache.height.toFloat() / this.textShapeCache.width.toFloat()

                if (placedChar.shouldMirror) {
                    textCacheArea = TextCacheArea(
                        minX = textCacheArea.maxX,
                        minY = textCacheArea.minY,
                        maxX = textCacheArea.minX,
                        maxY = textCacheArea.maxY
                    )
                }

                val operationSize = 8
                val claimedBufferSpace = this.claimSpace(numVertices = 6, numOperationValues = 4 * operationSize)
                if (claimedBufferSpace.didHardFlush) {
                    textCacheArea = this.textShapeCache.prepareCharacter(
                        placedChar.codepoint, font.ascent, font.descent,
                        glyphShape, cachedWidth, cachedHeight
                    )
                    if (textCacheArea == null) {
                        throw IllegalArgumentException("Can't draw character with codepoint ${placedChar.codepoint} after a hard flush")
                    }
                }

                this.pushRect(
                    placedChar.minX, placedChar.minY, placedChar.maxX, placedChar.maxY,
                    claimedBufferSpace.vertexIndex, claimedBufferSpace.depth,
                    claimedBufferSpace.operationIndex, claimedBufferSpace.operationIndex + operationSize,
                    claimedBufferSpace.operationIndex + 2 * operationSize,
                    claimedBufferSpace.operationIndex + 3 * operationSize
                )


                this.buffers.operationCpuBuffer.run {
                    for ((startOperationIndex, texX, texY) in arrayOf(
                        Triple(claimedBufferSpace.operationIndex, textCacheArea.minX, textCacheArea.minY),
                        Triple(claimedBufferSpace.operationIndex + operationSize, textCacheArea.maxX, textCacheArea.minY),
                        Triple(claimedBufferSpace.operationIndex + 2 * operationSize, textCacheArea.maxX, textCacheArea.maxY),
                        Triple(claimedBufferSpace.operationIndex + 3 * operationSize, textCacheArea.minX, textCacheArea.maxY)
                    )) {
                        this.put(startOperationIndex, OP_CODE_DRAW_TEXT)
                        this.put(startOperationIndex + 1, encodeFloat(texX))
                        this.put(startOperationIndex + 2, encodeFloat(texY))
                        this.put(startOperationIndex + 3, style.fillColor.rawValue)
                        this.put(startOperationIndex + 4, backgroundColor.rawValue)
                        this.put(startOperationIndex + 5, style.strokeColor.rawValue)
                        this.put(startOperationIndex + 6, encodeFloat(strokeDeltaX))
                        this.put(startOperationIndex + 7, encodeFloat(strokeDeltaY))
                    }
                }
            }
        }
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?): Float {
        val font = this.instance.fontManager.getFont(fontReference)
        val totalHeight = font.ascent - font.descent

        var totalWidth = 0

        var lastCodepoint = -1
        for (codepoint in string.codePoints()) {
            if (lastCodepoint != -1) totalWidth += font.getExtraAdvance(lastCodepoint, codepoint)
            totalWidth += font.getGlyphShape(codepoint).advanceWidth
            lastCodepoint = codepoint
        }

        return totalWidth.toFloat() / totalHeight.toFloat()
    }

    override fun getAspectRatio() = this.width.toFloat() / this.height.toFloat()

    fun setManualDepth(newDepth: Int) {
        if (depthPolicy != DepthPolicy.Manual) {
            throw UnsupportedOperationException("setManualDepth is only allowed when depthMode is Manual")
        }
        if (newDepth > maxDepth) {
            throw IllegalArgumentException("Can't set depth to $newDepth because the maxDepth is $maxDepth.")
        }
        currentDepth = newDepth
    }

    fun copyColorImageTo(
        destImage: Long?, destBuffer: Long?, destImageFormat: Int?,
        signalSemaphore: Long? = null, submissionMarker: CompletableDeferred<Unit>? = null,
        originalImageLayout: Int? = null, finalImageLayout: Int? = null,
        imageSrcAccessMask: Int? = null, imageSrcStageMask: Int? = null,
        imageDstAccessMask: Int? = null, imageDstStageMask: Int? = null
    ) {
        hardFlush()
        commands.copyColorImageTo(
            destImage = destImage, destBuffer = destBuffer, destImageFormat = destImageFormat,
            signalSemaphore = signalSemaphore, submissionMarker = submissionMarker,
            originalImageLayout = originalImageLayout, finalImageLayout = finalImageLayout,
            imageSrcAccessMask = imageSrcAccessMask, imageSrcStageMask = imageSrcStageMask,
            imageDstAccessMask = imageDstAccessMask, imageDstStageMask = imageDstStageMask
        )
    }

    fun destroy() {
        commands.destroy()
        textShapeCache.destroy()
        targetImages.destroy()
        descriptors.destroy()
        buffers.destroy()
    }
}
