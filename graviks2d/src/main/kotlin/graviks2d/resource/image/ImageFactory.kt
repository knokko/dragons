package graviks2d.resource.image

import graviks2d.core.GraviksInstance
import org.lwjgl.stb.STBImage.stbi_info_from_memory
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.stb.STBImageWrite.stbi_write_png_to_func
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*
import troll.exceptions.VulkanFailureException.assertVkSuccess
import troll.images.VmaImage
import troll.sync.ResourceUsage
import java.io.ByteArrayInputStream
import java.io.InputStream

internal fun createImagePair(
    instance: GraviksInstance, imageInput: InputStream, pathDescription: String
): VmaImage {
    val imageByteArray = imageInput.readAllBytes()

    val imageRawByteBuffer = memCalloc(imageByteArray.size)
    imageRawByteBuffer.put(0, imageByteArray)

    val width: Int
    val height: Int

    val name: String = if (pathDescription.contains("/"))
        pathDescription.substring(pathDescription.lastIndexOf('/') + 1)
    else pathDescription

    return stackPush().use { stack ->
        val pWidth = stack.callocInt(1)
        val pHeight = stack.callocInt(1)
        val pComponents = stack.callocInt(1)

        if (!stbi_info_from_memory(imageRawByteBuffer, pWidth, pHeight, pComponents)) {
            throw IllegalArgumentException("Can't decode image info at path $pathDescription")
        }

        width = pWidth[0]
        height = pHeight[0]
        val imagePixelByteBuffer = stbi_load_from_memory(imageRawByteBuffer, pWidth, pHeight, pComponents, 4)
            ?: throw IllegalArgumentException("Can't decode image at path $pathDescription")

        val image = instance.troll.images.createSimple(
            stack, width, height, VK_FORMAT_R8G8B8A8_UNORM, VK_SAMPLE_COUNT_1_BIT,
            VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT, name
        )

        val stagingBuffer = instance.troll.buffers.createMapped(
            imagePixelByteBuffer.capacity().toLong(),
            VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "$name-staging"
        )

        val stagingByteBuffer = memByteBuffer(stagingBuffer.hostAddress, imagePixelByteBuffer.capacity())
        memCopy(imagePixelByteBuffer, stagingByteBuffer)

        val commandPool = instance.troll.commands.createPool(
            VK_COMMAND_POOL_CREATE_TRANSIENT_BIT, instance.troll.queueFamilies().graphics.index, "$name-transfer"
        )
        val commandBuffer = instance.troll.commands.createPrimaryBuffers(commandPool, 1, "$name-transfer")[0]
        instance.troll.commands.begin(commandBuffer, stack, "$name-transfer")
        instance.troll.commands.transitionColorLayout(
            stack, commandBuffer, image.vkImage, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            null, ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
        )
        instance.troll.commands.copyBufferToImage(
            commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, image.vkImage, width, height, stagingBuffer.buffer.vkBuffer
        )
        instance.troll.commands.transitionColorLayout(
            stack, commandBuffer, image.vkImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
            ResourceUsage(VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
        )

        assertVkSuccess(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer", "GraviksImageFactory-$name")

        val fence = instance.troll.sync.createFences(false, 1, "fence-transfer-$name")[0]

        instance.troll.queueFamilies().graphics.queues.random().submit(
            commandBuffer, "ImageFactory.createImagePair-$name", emptyArray(), fence
        )

        assertVkSuccess(
            vkWaitForFences(instance.troll.vkDevice(), stack.longs(fence), true, 10_000_000_000L),
            "vkWaitForFences", "ImageFactory.createImagePair-$name"
        )
        vkDestroyFence(instance.troll.vkDevice(), fence, null)
        vkDestroyCommandPool(instance.troll.vkDevice(), commandPool, null)

        memFree(imagePixelByteBuffer)
        vmaDestroyBuffer(instance.troll.vmaAllocator(), stagingBuffer.buffer.vkBuffer, stagingBuffer.buffer.vmaAllocation)
        memFree(imageRawByteBuffer)

        image
    }
}

internal fun createDummyImage(instance: GraviksInstance): VmaImage {
    val singlePixelData = memCalloc(4)
    var dummyInput: ByteArrayInputStream? = null
    if (!stbi_write_png_to_func({ _, address, size ->
        val singlePixelPngData = memByteBuffer(address, size)
        val singlePixelPngArray = ByteArray(singlePixelPngData.capacity())
        singlePixelPngData.get(0, singlePixelPngArray)
        dummyInput = ByteArrayInputStream(singlePixelPngArray)
    }, 0L, 1, 1, 4, singlePixelData, 0)) {
        throw RuntimeException("stbi_write_png_to_func failed")
    }
    memFree(singlePixelData)

    return createImagePair(instance, dummyInput!!, "DummyImage")
}
