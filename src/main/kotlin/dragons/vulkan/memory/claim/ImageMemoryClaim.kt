package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.function.Supplier

class ImageMemoryClaim(
    val width: Int, val height: Int, val queueFamily: QueueFamily?,
    val imageCreateFlags: Int,
    val imageViewFlags: Int,
    val bytesPerPixel: Int,
    val imageFormat: Int,
    val tiling: Int,
    val samples: Int,
    val imageUsage: Int,
    /**
     * The game image loader will ensure that the image has this layout when it calls `storeResult`. This doesn't have
     * to be `VK_IMAGE_LAYOUT_UNDEFINED` or `VK_IMAGE_LAYOUT_PREINITIALIZED`; If you pick something else, the image
     * loader will perform a layout transition before calling `storeResult`.
     */
    val initialLayout: Int,
    val aspectMask: Int,
    val accessMask: Int,
    val prefill: ((ByteBuffer) -> Unit)?,
    val storeResult: CompletableDeferred<VulkanImage>
) {
    init {
        if (width <= 0) throw IllegalArgumentException("Width ($width) must be positive")
        if (height <= 0) throw IllegalArgumentException("Height ($height) must be positive")
        if (bytesPerPixel <= 0) throw IllegalArgumentException("bytesPerPixel ($bytesPerPixel) must be positive")
    }

    fun getNumPixels() = width * height

    fun getByteSize() = getNumPixels() * bytesPerPixel
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
