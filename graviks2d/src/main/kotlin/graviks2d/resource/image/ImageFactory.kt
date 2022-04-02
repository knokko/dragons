package graviks2d.resource.image

import graviks2d.core.GraviksInstance
import graviks2d.util.assertSuccess
import org.lwjgl.stb.STBImage.stbi_info_from_memory
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

internal fun createImageView(vkDevice: VkDevice, vkImage: Long): Long {
    return stackPush().use { stack ->
        val ciImageView = VkImageViewCreateInfo.calloc(stack)
        ciImageView.`sType$Default`()
        ciImageView.image(vkImage)
        ciImageView.viewType(VK_IMAGE_VIEW_TYPE_2D)
        ciImageView.format(VK_FORMAT_R8G8B8A8_UNORM)
        ciImageView.components().set(
            VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY
        )
        ciImageView.subresourceRange {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.levelCount(1)
            it.layerCount(1)
            it.baseMipLevel(0)
            it.baseArrayLayer(0)
        }

        val pImageView = stack.callocLong(1)
        assertSuccess(
            vkCreateImageView(vkDevice, ciImageView, null, pImageView),
            "vkCreateImageView"
        )

        pImageView[0]
    }
}

internal fun createImagePair(
    instance: GraviksInstance, imageInput: InputStream, pathDescription: String
): ImagePair {
    val imageByteArray = imageInput.readAllBytes()

    val imageRawByteBuffer = memCalloc(imageByteArray.size)
    imageRawByteBuffer.put(0, imageByteArray)

    val (image, allocation) = stackPush().use { stack ->
        val pWidth = stack.callocInt(1)
        val pHeight = stack.callocInt(1)
        val pComponents = stack.callocInt(1)

        if (!stbi_info_from_memory(imageRawByteBuffer, pWidth, pHeight, pComponents)) {
            throw IllegalArgumentException("Can't decode image info at path $pathDescription")
        }

        val width = pWidth[0]
        val height = pHeight[0]
        val imagePixelByteBuffer = stbi_load_from_memory(imageRawByteBuffer, pWidth, pHeight, pComponents, 4)
            ?: throw IllegalArgumentException("Can't decode image at path $pathDescription")

        val ciImage = VkImageCreateInfo.calloc(stack)
        ciImage.`sType$Default`()
        ciImage.imageType(VK_IMAGE_TYPE_2D)
        ciImage.format(VK_FORMAT_R8G8B8A8_UNORM)
        ciImage.extent().set(width, height, 1)
        ciImage.mipLevels(1)
        ciImage.arrayLayers(1)
        ciImage.samples(VK_SAMPLE_COUNT_1_BIT)
        ciImage.tiling(VK_IMAGE_TILING_OPTIMAL)
        ciImage.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
        ciImage.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        ciImage.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)

        val ciImageAllocation = VmaAllocationCreateInfo.calloc(stack)
        ciImageAllocation.flags(0)
        ciImageAllocation.usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE)

        val pImage = stack.callocLong(1)
        val pImageAllocation = stack.callocPointer(1)
        assertSuccess(
            vmaCreateImage(instance.vmaAllocator, ciImage, ciImageAllocation, pImage, pImageAllocation, null),
            "vmaCreateImage"
        )
        val image = pImage[0]
        val imageAllocation = pImageAllocation[0]

        val ciStagingBuffer = VkBufferCreateInfo.calloc(stack)
        ciStagingBuffer.`sType$Default`()
        ciStagingBuffer.size(imagePixelByteBuffer.capacity().toLong())
        ciStagingBuffer.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
        ciStagingBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

        val ciStagingBufferAllocation = VmaAllocationCreateInfo.calloc(stack)
        ciStagingBufferAllocation.flags(
            VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT
                    or VMA_ALLOCATION_CREATE_MAPPED_BIT
        )
        ciStagingBufferAllocation.usage(VMA_MEMORY_USAGE_AUTO)

        val pStagingBuffer = stack.callocLong(1)
        val pStagingAllocation = stack.callocPointer(1)
        val pAllocationInfo = VmaAllocationInfo.calloc(stack)
        assertSuccess(
            vmaCreateBuffer(
                instance.vmaAllocator, ciStagingBuffer, ciStagingBufferAllocation,
                pStagingBuffer, pStagingAllocation, pAllocationInfo
            ),
            "vmaCreateBuffer"
        )
        val stagingBuffer = pStagingBuffer[0]
        val stagingAllocation = pStagingAllocation[0]

        val stagingByteBuffer = memByteBuffer(pAllocationInfo.pMappedData(), imagePixelByteBuffer.capacity())
        memCopy(imagePixelByteBuffer, stagingByteBuffer)

        val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
        ciCommandPool.`sType$Default`()
        ciCommandPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
        ciCommandPool.queueFamilyIndex(instance.queueFamilyIndex)

        val pCommandPool = stack.callocLong(1)
        assertSuccess(
            vkCreateCommandPool(instance.device, ciCommandPool, null, pCommandPool),
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
            vkAllocateCommandBuffers(instance.device, aiCommandBuffer, pCommandBuffer),
            "vkAllocateCommandBuffers"
        )
        val commandBuffer = VkCommandBuffer(pCommandBuffer[0], instance.device)

        val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
        biCommandBuffer.`sType$Default`()
        biCommandBuffer.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

        assertSuccess(
            vkBeginCommandBuffer(commandBuffer, biCommandBuffer),
            "vkBeginCommandBuffer"
        )

        val preCopyBarriers = VkImageMemoryBarrier.calloc(1, stack)
        val preCopyBarrier = preCopyBarriers[0]
        preCopyBarrier.`sType$Default`()
        preCopyBarrier.srcAccessMask(0)
        preCopyBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
        preCopyBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        preCopyBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        preCopyBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        preCopyBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        preCopyBarrier.image(image)
        preCopyBarrier.subresourceRange {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.baseMipLevel(0)
            it.levelCount(1)
            it.baseArrayLayer(0)
            it.layerCount(1)
        }

        vkCmdPipelineBarrier(
            commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
            0, null, null, preCopyBarriers
        )

        val copyRegions = VkBufferImageCopy.calloc(1, stack)
        val copyRegion = copyRegions[0]
        copyRegion.bufferOffset(0)
        copyRegion.bufferRowLength(width)
        copyRegion.bufferImageHeight(height)
        copyRegion.imageSubresource {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.layerCount(1)
            it.baseArrayLayer(0)
            it.mipLevel(0)
        }
        copyRegion.imageOffset().set(0, 0, 0)
        copyRegion.imageExtent().set(width, height, 1)

        vkCmdCopyBufferToImage(commandBuffer, stagingBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegions)

        val postCopyBarriers = VkImageMemoryBarrier.calloc(1, stack)
        val postCopyBarrier = postCopyBarriers[0]
        postCopyBarrier.`sType$Default`()
        postCopyBarrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
        postCopyBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        postCopyBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        postCopyBarrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
        postCopyBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        postCopyBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        postCopyBarrier.image(image)
        postCopyBarrier.subresourceRange {
            it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            it.baseMipLevel(0)
            it.levelCount(1)
            it.baseArrayLayer(0)
            it.layerCount(1)
        }

        vkCmdPipelineBarrier(
            commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            0, null, null, postCopyBarriers
        )

        assertSuccess(
            vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer"
        )

        val pSubmitInfo = VkSubmitInfo.calloc(1, stack)
        val submitInfo = pSubmitInfo[0]
        submitInfo.`sType$Default`()
        submitInfo.waitSemaphoreCount(0)
        submitInfo.pCommandBuffers(stack.pointers(commandBuffer.address()))
        submitInfo.pSignalSemaphores(null)

        val ciFence = VkFenceCreateInfo.calloc(stack)
        ciFence.`sType$Default`()

        val pFence = stack.callocLong(1)
        assertSuccess(
            vkCreateFence(instance.device, ciFence, null, pFence),
            "vkCreateFence"
        )

        assertSuccess(
            instance.synchronizedQueueSubmit(pSubmitInfo, pFence[0]),
            "synchronizedQueueSubmit"
        )

        assertSuccess(
            vkWaitForFences(instance.device, pFence, true, 10_000_000_000L),
            "vkWaitForFences"
        )
        vkDestroyFence(instance.device, pFence[0], null)
        vkDestroyCommandPool(instance.device, commandPool, null)

        memFree(imagePixelByteBuffer)
        vmaDestroyBuffer(instance.vmaAllocator, stagingBuffer, stagingAllocation)
        memFree(imageRawByteBuffer)

        Pair(image, imageAllocation)
    }

    val imageView = createImageView(instance.device, image)
    return ImagePair(vkImage = image, vkImageView = imageView, vmaAllocation = allocation)
}

internal fun createDummyImage(instance: GraviksInstance): ImagePair {
    val dummyBufferedImage = BufferedImage(1, 1, TYPE_INT_ARGB)
    dummyBufferedImage.setRGB(0, 0, java.awt.Color(200, 150, 0).rgb)

    val dummyOutput = ByteArrayOutputStream()
    ImageIO.write(dummyBufferedImage, "PNG", dummyOutput)

    val dummyInput = ByteArrayInputStream(dummyOutput.toByteArray())
    return createImagePair(instance, dummyInput, "DummyImage")
}

internal class ImagePair(
    val vkImage: Long, val vkImageView: Long, val vmaAllocation: Long
)
