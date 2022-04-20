package graviks2d.resource.text

import graviks2d.context.GraviksContext
import graviks2d.pipeline.text.*
import graviks2d.pipeline.text.OPERATION_CORRECT_CONTROL
import graviks2d.pipeline.text.OPERATION_CORRECT_END
import graviks2d.pipeline.text.OPERATION_CORRECT_START
import graviks2d.pipeline.text.OPERATION_INCREMENT
import graviks2d.pipeline.text.TextCountVertex
import graviks2d.pipeline.text.TextVertexBuffer
import graviks2d.util.assertSuccess
import org.lwjgl.stb.STBRPContext
import org.lwjgl.stb.STBRPNode
import org.lwjgl.stb.STBRPRect
import org.lwjgl.stb.STBRectPack.stbrp_init_target
import org.lwjgl.stb.STBRectPack.stbrp_pack_rects
import org.lwjgl.stb.STBTruetype.STBTT_vcurve
import org.lwjgl.stb.STBTruetype.STBTT_vline
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.system.MemoryUtil.memFloatBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkImageViewCreateInfo

internal class TextShapeCache(
    val context: GraviksContext,
    val width: Int,
    val height: Int,
    private val vertexBufferSize: Int,
    rectanglePackingBufferSize: Int,
    rectanglePackingNodeBufferSize: Int
) {

    val textCountAtlas: Long
    val textCountAtlasView: Long
    private val textCountAtlasAllocation: Long

    val textOddAtlas: Long
    val textOddAtlasView: Long
    private val textOddAtlasAllocation: Long

    val textAtlasFramebuffer: Long

    val countVertexBuffer: Long
    private val countVertexBufferAllocation: Long
    private val hostVertexBuffer: TextVertexBuffer

    val oddVertexBuffer: Long
    private val oddVertexBufferAllocation: Long

    val descriptorPool: Long
    val descriptorSet: Long

    private val rectanglePackingContext: STBRPContext
    private val rectanglePackingBuffer: STBRPRect.Buffer
    private val rectanglePackingNodes: STBRPNode.Buffer
    private var currentRectanglePackingIndex = 0

    private val cachedCharacters = mutableMapOf<TextCacheKey, TextCacheArea>()
    var currentVertexIndex = 0

    init {
        /*
         * The size of the text cache must be at least as large as the size of the
         * context. If it were smaller, it would not be able to draw very big
         * characters that fill the entire context image.
         */
        if (this.width < this.context.width) {
            throw IllegalArgumentException("Width ($width) can't be smaller than context width ${this.context.width}")
        }
        if (this.height < this.context.height) {
            throw IllegalArgumentException("Height ($height) can't be smaller than context height ${this.context.height}")
        }

        this.rectanglePackingNodes = STBRPNode.calloc(rectanglePackingNodeBufferSize)

        this.rectanglePackingContext = STBRPContext.calloc()
        stbrp_init_target(this.rectanglePackingContext, width, height, this.rectanglePackingNodes)
        this.rectanglePackingBuffer = STBRPRect.calloc(rectanglePackingBufferSize)

        stackPush().use { stack ->
            val vmaAllocator = this.context.instance.vmaAllocator
            val vkDevice = this.context.instance.device

            val ciTextAtlas = VkImageCreateInfo.calloc(stack)
            ciTextAtlas.`sType$Default`()
            ciTextAtlas.imageType(VK_IMAGE_TYPE_2D)
            ciTextAtlas.format(TEXT_COLOR_FORMAT)
            ciTextAtlas.extent().set(this.width, this.height, 1)
            ciTextAtlas.mipLevels(1)
            ciTextAtlas.arrayLayers(1)
            ciTextAtlas.samples(VK_SAMPLE_COUNT_1_BIT)
            ciTextAtlas.tiling(VK_IMAGE_TILING_OPTIMAL)
            ciTextAtlas.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            ciTextAtlas.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

            val ciTextAtlasAllocation = VmaAllocationCreateInfo.calloc(stack)
            ciTextAtlasAllocation.usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE)

            val pTextAtlas = stack.callocLong(1)
            val pTextAtlasAllocation = stack.callocPointer(1)
            ciTextAtlas.usage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT)
            assertSuccess(
                vmaCreateImage(vmaAllocator, ciTextAtlas, ciTextAtlasAllocation, pTextAtlas, pTextAtlasAllocation, null),
                "vmaCreateImage"
            )
            this.textCountAtlas = pTextAtlas[0]
            this.textCountAtlasAllocation = pTextAtlasAllocation[0]

            ciTextAtlas.usage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
            assertSuccess(
                vmaCreateImage(vmaAllocator, ciTextAtlas, ciTextAtlasAllocation, pTextAtlas, pTextAtlasAllocation, null),
                "vmaCreateImage"
            )
            this.textOddAtlas = pTextAtlas[0]
            this.textOddAtlasAllocation = pTextAtlasAllocation[0]

            val ciTextAtlasView = VkImageViewCreateInfo.calloc(stack)
            ciTextAtlasView.`sType$Default`()
            // Set the .image field later
            ciTextAtlasView.viewType(VK_IMAGE_VIEW_TYPE_2D)
            ciTextAtlasView.format(TEXT_COLOR_FORMAT)
            ciTextAtlasView.components().set(
                VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY
            )
            ciTextAtlasView.subresourceRange {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseMipLevel(0)
                it.levelCount(1)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }

            val pTextAtlasView = stack.callocLong(1)
            ciTextAtlasView.image(this.textCountAtlas)
            assertSuccess(
                vkCreateImageView(vkDevice, ciTextAtlasView, null, pTextAtlasView),
                "vkCreateImageView"
            )
            this.textCountAtlasView = pTextAtlasView[0]

            ciTextAtlasView.image(this.textOddAtlas)
            assertSuccess(
                vkCreateImageView(vkDevice, ciTextAtlasView, null, pTextAtlasView),
                "vkCreateImageView"
            )
            this.textOddAtlasView = pTextAtlasView[0]

            val ciVertexBuffer = VkBufferCreateInfo.calloc(stack)
            ciVertexBuffer.`sType$Default`()
            val vertexBufferByteSize = this.vertexBufferSize.toLong() * TextCountVertex.BYTE_SIZE
            if (vertexBufferByteSize > Int.MAX_VALUE) {
                throw IllegalArgumentException("vertexBufferSize ($vertexBufferSize vertices -> $vertexBufferByteSize bytes) is too large")
            }
            // the .size field will be set later
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

            ciVertexBuffer.size(vertexBufferByteSize)
            assertSuccess(
                vmaCreateBuffer(vmaAllocator, ciVertexBuffer, ciVertexAllocation, pVertexBuffer, pVertexAllocation, vertexInfo),
                "vmaCreateBuffer"
            )
            this.countVertexBuffer = pVertexBuffer[0]
            this.countVertexBufferAllocation = pVertexAllocation[0]
            val hostVertexByteBuffer = memByteBuffer(vertexInfo.pMappedData(), vertexBufferByteSize.toInt())
            this.hostVertexBuffer = TextVertexBuffer.createAtBuffer(hostVertexByteBuffer, this.vertexBufferSize)

            val oddVertexBufferSize = 6L * 2 * 4
            ciVertexBuffer.size(oddVertexBufferSize)
            assertSuccess(
                vmaCreateBuffer(vmaAllocator, ciVertexBuffer, ciVertexAllocation, pVertexBuffer, pVertexAllocation, vertexInfo),
                "vmaCreateBuffer"
            )
            this.oddVertexBuffer = pVertexBuffer[0]
            this.oddVertexBufferAllocation = pVertexAllocation[0]
            val hostVertexOddByteBuffer = memFloatBuffer(vertexInfo.pMappedData(), 6 * 2)
            hostVertexOddByteBuffer.put(0, -1f)
            hostVertexOddByteBuffer.put(1, -1f)
            hostVertexOddByteBuffer.put(2, 1f)
            hostVertexOddByteBuffer.put(3, -1f)
            hostVertexOddByteBuffer.put(4, 1f)
            hostVertexOddByteBuffer.put(5, 1f)
            hostVertexOddByteBuffer.put(6, 1f)
            hostVertexOddByteBuffer.put(7, 1f)
            hostVertexOddByteBuffer.put(8, -1f)
            hostVertexOddByteBuffer.put(9, 1f)
            hostVertexOddByteBuffer.put(10, -1f)
            hostVertexOddByteBuffer.put(11, -1f)

            val ciFramebuffer = VkFramebufferCreateInfo.calloc(stack)
            ciFramebuffer.`sType$Default`()
            ciFramebuffer.renderPass(this.context.instance.textPipelines.vkRenderPass)
            ciFramebuffer.attachmentCount(2)
            ciFramebuffer.pAttachments(stack.longs(this.textCountAtlasView, this.textOddAtlasView))
            ciFramebuffer.width(this.width)
            ciFramebuffer.height(this.height)
            ciFramebuffer.layers(1)

            val pFramebuffer = stack.callocLong(1)
            assertSuccess(
                vkCreateFramebuffer(vkDevice, ciFramebuffer, null, pFramebuffer),
                "vkCreateFramebuffer"
            )
            this.textAtlasFramebuffer = pFramebuffer[0]

            val (descriptorPool, descriptorSet) = createTextOddPipelineDescriptors(
                vkDevice, stack, this.context.instance.textPipelines.oddDescriptorSetLayout,
                this.textCountAtlasView
            )
            this.descriptorPool = descriptorPool
            this.descriptorSet = descriptorSet
        }
    }

    private fun claimCacheArea(width: Int, height: Int): TextCacheArea? {
        if (this.currentRectanglePackingIndex == this.rectanglePackingBuffer.capacity()) {
            return null
        }

        val claimedRectangleIndex = this.currentRectanglePackingIndex
        this.currentRectanglePackingIndex += 1
        this.rectanglePackingBuffer.position(claimedRectangleIndex)
        this.rectanglePackingBuffer.limit(this.currentRectanglePackingIndex)

        this.rectanglePackingBuffer[claimedRectangleIndex].w(width)
        this.rectanglePackingBuffer[claimedRectangleIndex].h(height)

        val succeeded = stbrp_pack_rects(this.rectanglePackingContext, this.rectanglePackingBuffer)

        if (succeeded == 0) return null
        if (!this.rectanglePackingBuffer[claimedRectangleIndex].was_packed()) {
            throw RuntimeException("Rectangle should have been packed")
        }

        val x = this.rectanglePackingBuffer[claimedRectangleIndex].x()
        val y = this.rectanglePackingBuffer[claimedRectangleIndex].y()

        return TextCacheArea(
            x.toFloat() / this.width.toFloat(),
            y.toFloat() / this.height.toFloat(),
            (x + width).toFloat() / this.width.toFloat(),
            (y + height).toFloat() / this.height.toFloat()
        )
    }

    /**
     * Returns null if the given shape is not cached and doesn't fit in this cache.
     * In this case, the context will need to perform a hard flush, clear the cache,
     * and try again.
     */
    fun prepareCharacter(
        codepoint: Int, fontAscent: Int, fontDescent: Int,
        glyphShape: GlyphShape, width: Int, height: Int
    ): TextCacheArea? {
        if (glyphShape.ttfVertices == null) throw IllegalArgumentException("Don't use this method on empty glyphs")

        val cacheKey = TextCacheKey(
            codepoint, width, height
        )
        val cachedResult = this.cachedCharacters[cacheKey]
        if (cachedResult != null) return cachedResult

        var numVertices = 0
        for (ttfVertex in glyphShape.ttfVertices) {
            if (ttfVertex.type() == STBTT_vline) numVertices += 3
            if (ttfVertex.type() == STBTT_vcurve) numVertices += 6
        }

        if (this.currentVertexIndex + numVertices > this.vertexBufferSize) return null

        val cacheArea: TextCacheArea = this.claimCacheArea(width, height) ?: return null

        this.cachedCharacters[cacheKey] = cacheArea

        var prevX = 0f
        var prevY = 0f

        for (ttfVertex in glyphShape.ttfVertices) {
            ttfVertex.run {
                fun transformPoint(x: Short, y: Short): Pair<Float, Float> {
                    val localX = (x.toFloat() + 0.5f) / glyphShape.advanceWidth.toFloat()
                    val localY = (y.toFloat() + 0.5f - fontDescent.toFloat()) / (fontAscent - fontDescent).toFloat()

                    return Pair(
                        cacheArea.minX + localX * cacheArea.width,
                        cacheArea.minY + localY * cacheArea.height
                    )
                }

                val (currentX, currentY) = transformPoint(x(), y())

                if (type() == STBTT_vline || type() == STBTT_vcurve) {
                    hostVertexBuffer[currentVertexIndex].run {
                        this.x = prevX
                        this.y = prevY
                        this.operation = OPERATION_INCREMENT
                    }
                    hostVertexBuffer[currentVertexIndex + 1].run {
                        this.x = currentX
                        this.y = currentY
                        this.operation = OPERATION_INCREMENT
                    }
                    hostVertexBuffer[currentVertexIndex + 2].run {
                        this.x = 0f
                        this.y = 0f
                        this.operation = OPERATION_INCREMENT
                    }
                    currentVertexIndex += 3
                }
                if (type() == STBTT_vcurve) {
                    val (controlX, controlY) = transformPoint(cx(), cy())
                    hostVertexBuffer[currentVertexIndex].run {
                        this.x = prevX
                        this.y = prevY
                        this.operation = OPERATION_CORRECT_START
                    }
                    hostVertexBuffer[currentVertexIndex + 1].run {
                        this.x = controlX
                        this.y = controlY
                        this.operation = OPERATION_CORRECT_CONTROL
                    }
                    hostVertexBuffer[currentVertexIndex + 2].run {
                        this.x = currentX
                        this.y = currentY
                        this.operation = OPERATION_CORRECT_END
                    }
                    currentVertexIndex += 3
                }
                prevX = currentX
                prevY = currentY
            }
        }

        return cacheArea
    }

    fun clear() {
        this.currentVertexIndex = 0
        this.cachedCharacters.clear()
        this.currentRectanglePackingIndex = 0
        stbrp_init_target(this.rectanglePackingContext, this.width, this.height, this.rectanglePackingNodes)
    }

    fun destroy() {
        val vmaAllocator = this.context.instance.vmaAllocator
        val vkDevice = this.context.instance.device

        vkDestroyFramebuffer(vkDevice, this.textAtlasFramebuffer, null)
        vmaDestroyBuffer(vmaAllocator, this.countVertexBuffer, this.countVertexBufferAllocation)
        vmaDestroyBuffer(vmaAllocator, this.oddVertexBuffer, this.oddVertexBufferAllocation)
        vkDestroyImageView(vkDevice, this.textOddAtlasView, null)
        vmaDestroyImage(vmaAllocator, this.textOddAtlas, this.textOddAtlasAllocation)
        vkDestroyImageView(vkDevice, this.textCountAtlasView, null)
        vmaDestroyImage(vmaAllocator, this.textCountAtlas, this.textCountAtlasAllocation)
        vkDestroyDescriptorPool(vkDevice, this.descriptorPool, null)

        this.rectanglePackingBuffer.free()
        this.rectanglePackingNodes.free()
        this.rectanglePackingContext.free()
    }
}
