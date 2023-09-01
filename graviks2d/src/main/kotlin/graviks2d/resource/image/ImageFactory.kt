package graviks2d.resource.image

import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.images.VmaImage
import com.github.knokko.boiler.sync.ResourceUsage
import graviks2d.core.GraviksInstance
import org.lwjgl.stb.STBImage.stbi_info_from_memory
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.stb.STBImageWrite.stbi_write_png_to_func
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.vulkan.VK10.*
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

        val image = instance.boiler.images.createSimple(
            stack, width, height, VK_FORMAT_R8G8B8A8_UNORM,
            VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT, name
        )

        val stagingBuffer = instance.boiler.buffers.createMapped(
            imagePixelByteBuffer.capacity().toLong(),
            VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "$name-staging"
        )

        val stagingByteBuffer = memByteBuffer(stagingBuffer.hostAddress, imagePixelByteBuffer.capacity())
        memCopy(imagePixelByteBuffer, stagingByteBuffer)

        val commandPool = instance.boiler.commands.createPool(
            VK_COMMAND_POOL_CREATE_TRANSIENT_BIT, instance.boiler.queueFamilies().graphics.index, "$name-transfer"
        )
        val commandBuffer = instance.boiler.commands.createPrimaryBuffers(commandPool, 1, "$name-transfer")[0]
        instance.boiler.commands.begin(commandBuffer, stack, "$name-transfer")
        instance.boiler.commands.transitionColorLayout(
            stack, commandBuffer, image.vkImage, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            null, ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
        )
        instance.boiler.commands.copyBufferToImage(
            commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, image.vkImage, width, height, stagingBuffer.vkBuffer
        )
        instance.boiler.commands.transitionColorLayout(
            stack, commandBuffer, image.vkImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
            ResourceUsage(VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
        )

        assertVkSuccess(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer", "GraviksImageFactory-$name")

        val fence = instance.boiler.sync.createFences(false, 1, "fence-transfer-$name")[0]

        instance.boiler.queueFamilies().graphics.queues.random().submit(
            commandBuffer, "ImageFactory.createImagePair-$name", emptyArray(), fence
        )

        assertVkSuccess(
            vkWaitForFences(instance.boiler.vkDevice(), stack.longs(fence), true, 10_000_000_000L),
            "vkWaitForFences", "ImageFactory.createImagePair-$name"
        )
        vkDestroyFence(instance.boiler.vkDevice(), fence, null)
        vkDestroyCommandPool(instance.boiler.vkDevice(), commandPool, null)

        memFree(imagePixelByteBuffer)
        vmaDestroyBuffer(instance.boiler.vmaAllocator(), stagingBuffer.vkBuffer, stagingBuffer.vmaAllocation)
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
