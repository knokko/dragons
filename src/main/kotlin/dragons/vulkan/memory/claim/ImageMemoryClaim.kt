package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import org.lwjgl.vulkan.VK12.VK_IMAGE_LAYOUT_UNDEFINED
import org.lwjgl.vulkan.VK12.VK_SAMPLE_COUNT_1_BIT
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.function.Supplier

class ImageMemoryClaim(
    val width: Int, val height: Int, val queueFamily: QueueFamily?,
    val imageCreateFlags: Int = 0,
    val imageViewFlags: Int = 0,
    val bytesPerPixel: Int? = null,
    val imageFormat: Int,
    val tiling: Int,
    val samples: Int = VK_SAMPLE_COUNT_1_BIT,
    val imageUsage: Int,
    /**
     * The game image loader will ensure that the image has this layout when it calls `storeResult`. This doesn't have
     * to be `VK_IMAGE_LAYOUT_UNDEFINED` or `VK_IMAGE_LAYOUT_PREINITIALIZED`; If you pick something else, the image
     * loader will perform a layout transition before calling `storeResult`.
     */
    val initialLayout: Int,
    val aspectMask: Int,
    val accessMask: Int? = null,
    val dstPipelineStageMask: Int? = null,
    val storeResult: CompletableDeferred<VulkanImage>,
    val sharingID: String? = null,
    val prefill: ((ByteBuffer) -> Unit)?
) {
    init {
        if (width <= 0) throw IllegalArgumentException("Width ($width) must be positive")
        if (height <= 0) throw IllegalArgumentException("Height ($height) must be positive")
        if (prefill != null) {
            if (bytesPerPixel == null) throw IllegalArgumentException("You need to state the bytesPerPixel")
            if (bytesPerPixel <= 0) throw IllegalArgumentException("bytesPerPixel ($bytesPerPixel) must be positive")
        }
        if (prefill != null || initialLayout != VK_IMAGE_LAYOUT_UNDEFINED) {
            if (accessMask == null) throw IllegalArgumentException("You need to state the accessMask")
            if (dstPipelineStageMask == null) throw IllegalArgumentException("You need to state the dstPipelineStageMask")
        }
    }

    fun getNumPixels() = width * height

    /**
     * Note: this could fail if this is an uninitialized image claim
     */
    internal fun getStagingByteSize() = getNumPixels() * bytesPerPixel!!

    override fun toString() = "ImageMemoryClaim(width = $width, height = $height, queueFamily = $queueFamily)"
}

fun prefillBufferedImage(
    prefillImage: Supplier<BufferedImage>, width: Int, height: Int, bytesPerPixel: Int
): (ByteBuffer) -> Unit {
    return { destBuffer ->
        val sourceImage = prefillImage.get()

        if (sourceImage.width != width) {
            throw IllegalArgumentException("The promised width ($width) is not equal to the actual width (${sourceImage.width})")
        }
        if (sourceImage.height != height) {
            throw IllegalArgumentException("The promised height ($height) is not equal to the actual height (${sourceImage.height})")
        }

        if (bytesPerPixel < 1 || bytesPerPixel > 4) {
            throw IllegalArgumentException("bytesPerPixel ($bytesPerPixel) must be a number between 1 and 4")
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val destIndex = bytesPerPixel * (x + width * y)
                val sourceColor = Color(sourceImage.getRGB(x, y))

                destBuffer.put(destIndex, sourceColor.red.toByte())
                if (bytesPerPixel >= 2) {
                    destBuffer.put(destIndex + 1, sourceColor.green.toByte())
                }
                if (bytesPerPixel >= 3) {
                    destBuffer.put(destIndex + 2, sourceColor.blue.toByte())
                }
                if (bytesPerPixel == 4) {
                    destBuffer.put(destIndex + 3, sourceColor.alpha.toByte())
                }
            }
        }
    }
}
