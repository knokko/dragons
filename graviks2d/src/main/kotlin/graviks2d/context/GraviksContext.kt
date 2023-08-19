package graviks2d.context

import graviks2d.core.GraviksInstance
import graviks2d.pipeline.*
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_BOTTOM_LEFT
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_TOP_LEFT
import graviks2d.pipeline.OP_CODE_DRAW_IMAGE_TOP_RIGHT
import graviks2d.pipeline.OP_CODE_FILL
import graviks2d.resource.image.BorrowedImage
import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.*
import graviks2d.resource.text.TextShapeCache
import graviks2d.resource.text.placeText
import graviks2d.target.GraviksScissor
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.lwjgl.system.MemoryStack.stackPush
import troll.sync.ResourceUsage
import troll.sync.WaitSemaphore
import kotlin.IllegalStateException
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

class GraviksContext(
    val instance: GraviksInstance,
    val width: Int,
    val height: Int,

    /**
     * When creating a new `GraviksContext`, its target image will be filled with the `initialBackgroundColor`. You
     * can see this background color if you view the target image before doing any drawing operations or color clears.
     */
    initialBackgroundColor: Color = Color.rgbInt(100, 100, 100),

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

    private var currentVertexIndex = -1
    private var currentOperationIndex = -1

    private val commands = ContextCommands(this)
    private val currentImages = mutableMapOf<BorrowedImage, Int>()
    private val currentCustomImages = mutableMapOf<Long, Int>()
    internal val textShapeCache = TextShapeCache(
        context = this, width = textCacheWidth, height = textCacheHeight,
        vertexBufferSize = textVertexBufferSize, rectanglePackingBufferSize = textRectanglePackingBufferSize,
        rectanglePackingNodeBufferSize = textRectanglePackingNodeBufferSize
    )
    internal val descriptors = ContextDescriptors(
        this.instance, this.buffers.operationBuffer.buffer, this.textShapeCache.textOddAtlas.vkImageView
    )

    private val queuedDrawCommands: MutableList<DrawCommand> = mutableListOf()
    private val scissors = mutableMapOf<GraviksScissor, Int>()
    private var hasPendingCommand = false

    private var currentScissor: GraviksScissor

    init {
        if (vertexBufferSize < 6) throw IllegalArgumentException("vertexBufferSize must be at least 6")
        if (operationBufferSize < 6) throw IllegalArgumentException("operationBufferSize must be at least 6")

        currentScissor = GraviksScissor(0f, 0f, 1f, 1f)
        this.putScissor(currentScissor, 2)
        this.pushRect(0f, 0f, 1f, 1f, vertexIndex = 0, operationIndex = 0)

        this.buffers.operationCpuBuffer.run {
            this.put(0, 1) // 1 is the op code for fill rect
            this.put(1, initialBackgroundColor.rawValue)
        }
        currentVertexIndex = 6 // The first 6 vertices are used for the color clear
        currentOperationIndex = 6 // The first 6 operation values are used for the color clear and default scissor
    }

    private fun encodeFloat(value: Float) = (1_000_000.toFloat() * value).roundToInt()

    private fun putScissor(scissor: GraviksScissor, index: Int) {
        this.buffers.operationCpuBuffer.run {
            this.put(index, encodeFloat(scissor.minX))
            this.put(index + 1, encodeFloat(scissor.minY))
            this.put(index + 2, encodeFloat(scissor.maxX))
            this.put(index + 3, encodeFloat(scissor.maxY))
        }
        scissors[scissor] = index
    }

    private fun claimSpace(numVertices: Int, numOperationValues: Int): ContextSpaceClaim {
        handlePendingCommand()

        var opValuesToClaim = numOperationValues
        if (!scissors.containsKey(currentScissor)) opValuesToClaim += 4

        var (operationIndex, didOperationFlush) = this.claimNextOperationIndex(opValuesToClaim)

        // Note: claimNextVertexIndex MUST be called as last to avoid weird situations when a flush() occurs BEFORE
        // the vertices are actually populated.
        val (vertexIndex, didVertexFlush) = this.claimNextVertexIndex(numVertices)

        // If a vertex flush occurred, the operation index will be invalid and needs to be recomputed
        if (didVertexFlush) {
            // Note: claiming this operation index can NOT cause another hard flush because the operation buffer is empty

            opValuesToClaim = numOperationValues
            if (!scissors.containsKey(currentScissor)) opValuesToClaim += 4

            operationIndex = this.claimNextOperationIndex(opValuesToClaim).first
        }

        if (!scissors.containsKey(currentScissor)) {
            putScissor(currentScissor, operationIndex)
            operationIndex += 4
        }

        return ContextSpaceClaim(
            vertexIndex = vertexIndex,
            operationIndex = operationIndex,
            didHardFlush = didOperationFlush || didVertexFlush
        )
    }

    override fun setScissor(newScissor: GraviksScissor): GraviksScissor {
        val oldScissor = currentScissor
        this.currentScissor = newScissor
        return oldScissor
    }

    override fun getScissor() = this.currentScissor

    private fun claimNextVertexIndex(numVertices: Int): Pair<Int, Boolean> {
        var didHardFlush = false
        if (numVertices > this.buffers.vertexBufferSize) {
            throw IllegalArgumentException(
                "Too many vertices ($numVertices): at most ${this.buffers.vertexBufferSize} are allowed. " +
                        "Pass a larger value for vertexBufferSize to the constructor of this class to overwrite it."
            )
        }
        if (this.currentVertexIndex + numVertices > this.buffers.vertexBufferSize) {
            this.hardFlush(true)
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
            this.hardFlush(true)
            didHardFlush = true
            // hardFlush() should set currentOperationIndex to 0
        }
        val result = this.currentOperationIndex
        this.currentOperationIndex += numIntValues
        return Pair(result, didHardFlush)
    }

    private fun softFlush() {
        val firstVertexIndex = if (this.queuedDrawCommands.isEmpty()) { 0 } else {
            this.queuedDrawCommands.last().vertexIndex + this.queuedDrawCommands.last().numVertices
        }
        if (firstVertexIndex != this.currentVertexIndex) {
            this.queuedDrawCommands.add(
                DrawCommand(
                    vertexIndex = firstVertexIndex,
                    numVertices = this.currentVertexIndex - firstVertexIndex
                )
            )
        }
        this.currentVertexIndex = firstVertexIndex
    }

    private fun hardFlush(block: Boolean) {
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
                val imageViewsArray = Array<Long>(currentImages.size + currentCustomImages.size) { 0 }
                for ((borrowedImage, index) in currentImages) {
                    imageViewsArray[index] = borrowedImage.imagePair.await().vkImageView
                }
                for ((vkImageView, index) in currentCustomImages) {
                    imageViewsArray[index] = vkImageView
                }
                descriptors.updateDescriptors(imageViewsArray)
            }
            commands.draw(queuedDrawCommands, endSubmitAndWait = block)
            if (block) returnImages()
            queuedDrawCommands.clear()
        }

        currentVertexIndex = 0
        currentOperationIndex = 0
        scissors.clear()
        textShapeCache.clear()
    }

    private fun returnImages() {
        for (borrowedImage in currentImages.keys) {
            instance.imageCache.returnImage(borrowedImage)
        }
        currentImages.clear()
        currentCustomImages.clear()
    }

    private fun handlePendingCommand() {
        if (hasPendingCommand) {
            stackPush().use { stack -> commands.awaitPendingSubmission(stack) }
            returnImages()
            hasPendingCommand = false
        }
    }

    private fun pushRect(
        x1: Float, y1: Float, x2: Float, y2: Float, vertexIndex: Int, operationIndex: Int
    ) {
        this.pushRect(x1, y1, x2, y2, vertexIndex, operationIndex, operationIndex, operationIndex, operationIndex)
    }

    private fun pushTriangle(
        x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, vertexIndex: Int,
        operationIndex1: Int, operationIndex2: Int, operationIndex3: Int
    ) {
        val scissorIndex = scissors[currentScissor] ?: throw IllegalStateException("Current scissor is not stored")
        this.buffers.vertexCpuBuffer.run {
            this[vertexIndex].x = x1
            this[vertexIndex].y = y1
            this[vertexIndex].operationIndex = operationIndex1
            this[vertexIndex].scissorIndex = scissorIndex

            this[vertexIndex + 1].x = x2
            this[vertexIndex + 1].y = y2
            this[vertexIndex + 1].operationIndex = operationIndex2
            this[vertexIndex + 1].scissorIndex = scissorIndex

            this[vertexIndex + 2].x = x3
            this[vertexIndex + 2].y = y3
            this[vertexIndex + 2].operationIndex = operationIndex3
            this[vertexIndex + 2].scissorIndex = scissorIndex
        }
    }

    private fun pushTriangle(
        x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, vertexIndex: Int, operationIndex: Int
    ) {
        pushTriangle(x1, y1, x2, y2, x3, y3, vertexIndex, operationIndex, operationIndex, operationIndex)
    }

    private fun pushRect(
        x1: Float, y1: Float, x2: Float, y2: Float, vertexIndex: Int,
        operationIndex1: Int, operationIndex2: Int, operationIndex3: Int, operationIndex4: Int
    ) {
        pushTriangle(x1, y1, x2, y1, x2, y2, vertexIndex, operationIndex1, operationIndex2, operationIndex3)
        pushTriangle(x2, y2, x1, y2, x1, y1, vertexIndex + 3, operationIndex3, operationIndex4, operationIndex1)
    }

    override fun fillTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Color) {
        val claimedSpace = this.claimSpace(numVertices = 3, numOperationValues = 2)

        this.pushTriangle(x1, y1, x2, y2, x3, y3, vertexIndex = claimedSpace.vertexIndex, operationIndex = claimedSpace.operationIndex)

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_FILL)
            this.put(claimedSpace.operationIndex + 1, color.rawValue)
        }
    }

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 2)

        this.pushRect(x1, y1, x2, y2, vertexIndex = claimedSpace.vertexIndex, operationIndex = claimedSpace.operationIndex)

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_FILL)
            this.put(claimedSpace.operationIndex + 1, color.rawValue)
        }
    }

    override fun drawRect(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float, color: Color) {
        val xLeft = min(x1, x2) - lineWidth * 0.5f
        val yBottom = min(y1, y2) - lineWidth * 0.5f
        val xRight = max(x1, x2) + lineWidth * 0.5f
        val yTop = max(y1, y2) + lineWidth * 0.5f

        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 6)

        this.pushRect(
                xLeft, yBottom, xRight, yTop, vertexIndex = claimedSpace.vertexIndex,
                operationIndex1 = claimedSpace.operationIndex, operationIndex2 = claimedSpace.operationIndex + 4,
                operationIndex3 = claimedSpace.operationIndex + 5, operationIndex4 = claimedSpace.operationIndex + 6
        )

        val lineWidthX = lineWidth / (xRight - xLeft)
        val lineWidthY = lineWidth / (yTop - yBottom)

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_DRAW_RECT_BOTTOM_LEFT)
            this.put(claimedSpace.operationIndex + 1, encodeFloat(lineWidthX))
            this.put(claimedSpace.operationIndex + 2, encodeFloat(lineWidthY))
            this.put(claimedSpace.operationIndex + 3, color.rawValue)
            this.put(claimedSpace.operationIndex + 4, OP_CODE_DRAW_RECT_BOTTOM_RIGHT)
            this.put(claimedSpace.operationIndex + 5, OP_CODE_DRAW_RECT_TOP_RIGHT)
            this.put(claimedSpace.operationIndex + 6, OP_CODE_DRAW_RECT_TOP_LEFT)
        }
    }

    override fun drawRoundedRect(
        x1: Float, y1: Float, x2: Float, y2: Float, radiusX: Float, lineWidth: Float, color: Color
    ) {
        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 8)

        this.pushRect(x1, y1, x2, y2, vertexIndex = claimedSpace.vertexIndex, operationIndex = claimedSpace.operationIndex)

        this.buffers.operationCpuBuffer.run {
            this.put(claimedSpace.operationIndex, OP_CODE_DRAW_ROUNDED_RECT)
            this.put(claimedSpace.operationIndex + 1, color.rawValue)
            this.put(claimedSpace.operationIndex + 2, encodeFloat(min(x1, x2)))
            this.put(claimedSpace.operationIndex + 3, encodeFloat(min(y1, y2)))
            this.put(claimedSpace.operationIndex + 4, encodeFloat(max(x1, x2)))
            this.put(claimedSpace.operationIndex + 5, encodeFloat(max(y1, y2)))
            this.put(claimedSpace.operationIndex + 6, encodeFloat(radiusX))
            this.put(claimedSpace.operationIndex + 7, encodeFloat(lineWidth))
        }
    }

    private fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, imageIndex: Int) {
        val claimedSpace = this.claimSpace(numVertices = 6, numOperationValues = 5)

        this.pushRect(
            xLeft, yBottom, xRight, yTop, vertexIndex = claimedSpace.vertexIndex,
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

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        val borrowedImage = this.instance.imageCache.borrowImage(image)
        var imageIndex = this.currentImages[borrowedImage]
        if (imageIndex == null) {
            if (this.currentImages.size + this.currentCustomImages.size >= this.instance.maxNumDescriptorImages) {
                this.hardFlush(true)
            }
            imageIndex = this.currentImages.size + this.currentCustomImages.size
            this.currentImages[borrowedImage] = imageIndex
        }

        drawImage(xLeft, yBottom, xRight, yTop, imageIndex)
    }

    override fun drawVulkanImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, vkImage: Long, vkImageView: Long) {
        var imageIndex = this.currentCustomImages[vkImageView]
        if (imageIndex == null) {
            if (this.currentImages.size + this.currentCustomImages.size >= this.instance.maxNumDescriptorImages) {
                this.hardFlush(true)
            }
            imageIndex = this.currentImages.size + this.currentCustomImages.size
            this.currentCustomImages[vkImageView] = imageIndex
        }

        drawImage(xLeft, yBottom, xRight, yTop, imageIndex)
    }

    override fun addWaitSemaphore(semaphore: WaitSemaphore) {
        this.commands.addWaitSemaphore(semaphore)
    }

    override fun getImageSize(image: ImageReference): Pair<Int, Int> {
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
        string: String, style: TextStyle, dryRun: Boolean,
        suggestLeftToRight: Boolean
    ): List<CharacterPosition> {
        val font = this.instance.fontManager.getFont(style.font)
        val placedChars = placeText(minX, yBottom, maxX, yTop, string, style, font, this.width, this.height, suggestLeftToRight)

        if (!dryRun) {
            val oldScissor = this.getScissor()
            this.setScissor(GraviksScissor(minX, yBottom, maxX, yTop).combine(oldScissor))
            for (placedChar in placedChars) {
                font.borrowGlyphShape(placedChar.codepoint) { glyphShape ->
                    if (glyphShape.ttfVertices != null && placedChar.pixelWidth > 0 && placedChar.pixelHeight > 0) {

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

                        var textCacheArea = this.textShapeCache.prepareCharacter(
                            placedChar.codepoint, font.ascent, font.descent,
                            glyphShape, cachedWidth, cachedHeight
                        )
                        if (textCacheArea == null) {
                            this.hardFlush(true)
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
                            placedChar.position.minX, placedChar.position.minY,
                                placedChar.position.maxX, placedChar.position.maxY,
                            claimedBufferSpace.vertexIndex, claimedBufferSpace.operationIndex,
                                claimedBufferSpace.operationIndex + operationSize,
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
                                this.put(startOperationIndex + 3, style.getFillColor(placedChar.originalIndex).rawValue)
                                this.put(startOperationIndex + 4, style.getStrokeColor(placedChar.originalIndex).rawValue)
                                this.put(startOperationIndex + 5, encodeFloat(strokeDeltaX))
                                this.put(startOperationIndex + 6, encodeFloat(strokeDeltaY))
                            }
                        }
                    }
                }
            }

            this.setScissor(oldScissor)
        }
        return placedChars.sortedBy { it.originalIndex }.map { it.position }
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?): Float {
        val font = this.instance.fontManager.getFont(fontReference)
        val totalHeight = font.ascent - font.descent

        var totalWidth = 0

        var lastCodepoint = -1
        for (codepoint in string.codePoints()) {
            if (lastCodepoint != -1) totalWidth += font.getExtraAdvance(lastCodepoint, codepoint)
            totalWidth += font.getAdvanceWidth(codepoint)
            lastCodepoint = codepoint
        }

        return totalWidth.toFloat() / totalHeight.toFloat()
    }

    override fun getAspectRatio() = this.width.toFloat() / this.height.toFloat()

    fun awaitCompletion() {
        handlePendingCommand()
        hardFlush(true)
    }

    fun copyColorImageTo(
        destImage: Long?, destBuffer: Long?, destImageFormat: Int?,
        signalSemaphore: Long? = null, submissionMarker: CompletableDeferred<Unit>? = null,
        originalImageLayout: Int? = null, finalImageLayout: Int? = null,
        imageSrcUsage: ResourceUsage? = null, imageDstUsage: ResourceUsage? = null,
        shouldAwaitCompletion: Boolean
    ) {
        handlePendingCommand()
        hardFlush(false)
        commands.copyColorImageTo(
            destImage = destImage, destBuffer = destBuffer, destImageFormat = destImageFormat,
            signalSemaphore = signalSemaphore, submissionMarker = submissionMarker,
            originalImageLayout = originalImageLayout, finalImageLayout = finalImageLayout,
            imageSrcUsage = imageSrcUsage, imageDstUsage = imageDstUsage,
            shouldAwaitCompletion = shouldAwaitCompletion
        )
        hasPendingCommand = !shouldAwaitCompletion
        if (shouldAwaitCompletion) returnImages()
    }

    fun destroy() {
        commands.destroy()
        textShapeCache.destroy()
        targetImages.destroy()
        descriptors.destroy()
        buffers.destroy()
    }
}
