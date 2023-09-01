package dragons.plugins.standard.vulkan.render.entity

import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVmaSuccess
import dragons.state.StaticGraphicsState
import dragons.vulkan.memory.VulkanImage
import graviks2d.util.Color
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer
import javax.imageio.ImageIO

interface EntityColorImage {

    val width: Int

    val height: Int

    fun create(graphicsState: StaticGraphicsState): Pair<VulkanImage, Long>
}

interface EntityHeightImage {

    val width: Int

    val height: Int

    fun create(graphicsState: StaticGraphicsState): Pair<VulkanImage, Long>
}

private fun createPrefilledColorOrHeightImage(
    graphicsState: StaticGraphicsState, width: Int, height: Int, imageFormat: Int, bytesPerPixel: Long,
    fillFunction: (ByteBuffer) -> Unit
) = stackPush().use { stack ->
    val ciImage = VkImageCreateInfo.calloc(stack)
    ciImage.`sType$Default`()
    ciImage.imageType(VK_IMAGE_TYPE_2D)
    ciImage.format(imageFormat)
    ciImage.extent().set(width, height, 1)
    // TODO Add support for mipmapping
    ciImage.mipLevels(1)
    ciImage.arrayLayers(1)
    ciImage.samples(VK_SAMPLE_COUNT_1_BIT)
    ciImage.tiling(VK_IMAGE_TILING_OPTIMAL)
    ciImage.usage(VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)
    ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
    ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

    val ciImageAllocation = VmaAllocationCreateInfo.calloc(stack)
    ciImageAllocation.usage(VMA_MEMORY_USAGE_AUTO)

    val pImage = stack.callocLong(1)
    val pImageAllocation = stack.callocPointer(1)

    assertVmaSuccess(
        vmaCreateImage(graphicsState.boiler.vmaAllocator(), ciImage, ciImageAllocation, pImage, pImageAllocation, null),
        "CreateImage", "Prefilled entity"
    )
    val image = pImage[0]
    val imageAllocation = pImageAllocation[0]

    val ciBuffer = VkBufferCreateInfo.calloc(stack)
    ciBuffer.`sType$Default`()
    ciBuffer.size(bytesPerPixel * width * height)
    ciBuffer.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
    ciBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

    val ciBufferAllocation = VmaAllocationCreateInfo.calloc(stack)
    ciBufferAllocation.usage(VMA_MEMORY_USAGE_AUTO)
    ciBufferAllocation.flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or VMA_ALLOCATION_CREATE_MAPPED_BIT)

    val bufferInfo = VmaAllocationInfo.calloc(stack)

    val pBuffer = stack.callocLong(1)
    val pBufferAllocation = stack.callocPointer(1)

    assertVmaSuccess(
        vmaCreateBuffer(graphicsState.boiler.vmaAllocator(), ciBuffer, ciBufferAllocation, pBuffer, pBufferAllocation, bufferInfo),
        "CreateBuffer", "Prefilled entity (staging)"
    )
    val stagingBuffer = pBuffer[0]
    val bufferAllocation = pBufferAllocation[0]

    fillFunction(memByteBuffer(bufferInfo.pMappedData(), bytesPerPixel.toInt() * width * height))

    val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
    ciCommandPool.`sType$Default`()
    ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
    // TODO Try the transfer queue family instead
    ciCommandPool.queueFamilyIndex(graphicsState.queueManager.generalQueueFamily.index)

    val pCommandPool = stack.callocLong(1)
    assertVkSuccess(
        vkCreateCommandPool(graphicsState.boiler.vkDevice(), ciCommandPool, null, pCommandPool),
        "CreateCommandPool", "Entity image transfer"
    )
    val commandPool = pCommandPool[0]

    val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
    aiCommandBuffer.`sType$Default`()
    aiCommandBuffer.commandPool(commandPool)
    aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
    aiCommandBuffer.commandBufferCount(1)

    val pCommandBuffer = stack.callocPointer(1)

    assertVkSuccess(
        vkAllocateCommandBuffers(graphicsState.boiler.vkDevice(), aiCommandBuffer, pCommandBuffer),
        "AllocateCommandBuffers", "Entity image transfer"
    )
    val commandBuffer = VkCommandBuffer(pCommandBuffer[0], graphicsState.boiler.vkDevice())

    val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
    biCommandBuffer.`sType$Default`()
    biCommandBuffer.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

    assertVkSuccess(
        vkBeginCommandBuffer(commandBuffer, biCommandBuffer),
        "BeginCommandBuffer", "Entity image transfer"
    )

    val pBufferBarrier = VkBufferMemoryBarrier.calloc(1, stack)
    pBufferBarrier.`sType$Default`()
    pBufferBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT)
    pBufferBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
    pBufferBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
    pBufferBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
    pBufferBarrier.buffer(stagingBuffer)
    pBufferBarrier.offset(0)
    pBufferBarrier.size(VK_WHOLE_SIZE)

    val pImageBarrier = VkImageMemoryBarrier.calloc(1, stack)
    pImageBarrier.`sType$Default`()
    pImageBarrier.srcAccessMask(0)
    pImageBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
    pImageBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
    pImageBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
    pImageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
    pImageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
    pImageBarrier.image(image)
    pImageBarrier.subresourceRange {
        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        it.baseMipLevel(0)
        it.levelCount(1)
        it.baseArrayLayer(0)
        it.layerCount(1)
    }

    val pCopyRegion = VkBufferImageCopy.calloc(1, stack)
    pCopyRegion.bufferOffset(0)
    pCopyRegion.bufferRowLength(width)
    pCopyRegion.bufferImageHeight(height)
    pCopyRegion.imageSubresource {
        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        it.mipLevel(0)
        it.baseArrayLayer(0)
        it.layerCount(1)
    }
    pCopyRegion.imageOffset().set(0, 0, 0)
    pCopyRegion.imageExtent().set(width, height, 1)

    vkCmdPipelineBarrier(
        commandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
        null, pBufferBarrier, null
    )
    vkCmdPipelineBarrier(
        commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
        null, null, pImageBarrier
    )
    vkCmdCopyBufferToImage(
        commandBuffer, stagingBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pCopyRegion
    )

    pImageBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
    pImageBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
    pImageBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
    pImageBarrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    vkCmdPipelineBarrier(
        commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT,
        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT or VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT,
        0, null, null, pImageBarrier
    )

    assertVkSuccess(
        vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "Entity image transfer"
    )

    val ciFence = VkFenceCreateInfo.calloc(stack)
    ciFence.`sType$Default`()

    val pFence = stack.callocLong(1)
    assertVkSuccess(
        vkCreateFence(graphicsState.boiler.vkDevice(), ciFence, null, pFence),
        "CreateFence", "Entity image transfer"
    )
    val fence = pFence[0]

    graphicsState.queueManager.generalQueueFamily.getRandomBackgroundQueue().submit(
        commandBuffer, "Prefilled entity image", emptyArray(), fence
    )

    assertVkSuccess(
        vkWaitForFences(graphicsState.boiler.vkDevice(), pFence, true, 1_000_000_000L),
        "WaitForFences", "Entity image transfer"
    )
    vkDestroyFence(graphicsState.boiler.vkDevice(), fence, null)

    vmaDestroyBuffer(graphicsState.boiler.vmaAllocator(), stagingBuffer, bufferAllocation)

    vkDestroyCommandPool(graphicsState.boiler.vkDevice(), commandPool, null)

    val ciImageView = VkImageViewCreateInfo.calloc(stack)
    ciImageView.`sType$Default`()
    ciImageView.image(image)
    ciImageView.viewType(VK_IMAGE_VIEW_TYPE_2D)
    ciImageView.format(imageFormat)
    ciImageView.components().set(
        VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
        VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY
    )
    ciImageView.subresourceRange {
        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        it.baseMipLevel(0)
        it.levelCount(1) // TODO Add support for mipmapping
        it.baseArrayLayer(0)
        it.layerCount(1)
    }

    val pImageView = stack.callocLong(1)
    assertVkSuccess(
        vkCreateImageView(graphicsState.boiler.vkDevice(), ciImageView, null, pImageView),
        "CreateImageView", "Entity image transfer"
    )
    val imageView = pImageView[0]

    val imageWrapper = VulkanImage(image, width, height)
    imageWrapper.fullView = imageView
    Pair(imageWrapper, imageAllocation)
}

abstract class PrefilledEntityColorImage(
    override val width: Int,
    override val height: Int
): EntityColorImage {

    override fun create(graphicsState: StaticGraphicsState): Pair<VulkanImage, Long> {
        return createPrefilledColorOrHeightImage(
            graphicsState, this.width, this.height, VK_FORMAT_R8G8B8A8_SRGB, 4L
        ) { hostBuffer ->
            this.prefill { x, y, color ->
                val index = 4 * (x + y * width)
                hostBuffer.put(index, color.red.toByte())
                hostBuffer.put(index + 1, color.green.toByte())
                hostBuffer.put(index + 2, color.blue.toByte())
                hostBuffer.put(index + 3, color.alpha.toByte())
            }
        }
    }

    abstract fun prefill(setColor: (x: Int, y: Int, Color) -> Unit)
}

abstract class PrefilledEntityHeightImage(
    override val width: Int,
    override val height: Int
): EntityHeightImage {

    override fun create(graphicsState: StaticGraphicsState): Pair<VulkanImage, Long> {
        return createPrefilledColorOrHeightImage(
            graphicsState, this.width, this.height, VK_FORMAT_R32_SFLOAT, 4L
        ) { byteBuffer ->
            val floatBuffer = byteBuffer.asFloatBuffer()
            this.prefill { x, y, height ->
                run {
                    floatBuffer.put(x + y * this.width, height)
                }
            }
        }
    }

    abstract fun prefill(setHeight: (x: Int, y: Int, height: Float) -> Unit)
}

class ClasspathEntityColorImage(
    private val path: String, width: Int, height: Int
): PrefilledEntityColorImage(width, height) {
    override fun prefill(setColor: (x: Int, y: Int, Color) -> Unit) {

        // TODO Use STB instead of AWT
        val rawInput = ClasspathEntityColorImage::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Can't find resource at path $path")
        val bufferedImage = ImageIO.read(rawInput)

        for (y in 0 until this.height) {
            for (x in 0 until this.width) {
                val awtColor = java.awt.Color(bufferedImage.getRGB(x, y), true)
                setColor(x, y, Color.rgbaInt(awtColor.red, awtColor.green, awtColor.blue, awtColor.alpha))
            }
        }

        rawInput.close()
    }
}

class ClasspathEntityHeightImage(
    private val path: String, private val weight: Float, width: Int, height: Int
): PrefilledEntityHeightImage(width, height) {
    override fun prefill(setHeight: (x: Int, y: Int, height: Float) -> Unit) {

        // TODO Use STB instead of AWT
        val rawInput = ClasspathEntityHeightImage::class.java.classLoader.getResourceAsStream(this.path)
            ?: throw IllegalArgumentException("Can't find resource at path $path")
        val bufferedImage = ImageIO.read(rawInput)

        for (y in 0 until this.height) {
            for (x in 0 until this.width) {
                val awtColor = java.awt.Color(bufferedImage.getRGB(x, y), true)
                setHeight(x, y, this.weight * (awtColor.red - 127))
            }
        }

        rawInput.close()
    }
}