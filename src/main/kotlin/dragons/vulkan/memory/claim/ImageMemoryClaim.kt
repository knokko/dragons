package dragons.vulkan.memory.claim

import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.queue.QueueFamily
import kotlinx.coroutines.CompletableDeferred
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.function.Consumer
import java.util.function.Supplier

abstract class ImageMemoryClaim(
    val width: Int, val height: Int, val bytesPerPixel: Int, val queueFamily: QueueFamily?,
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

class PrefilledImageMemoryClaim(
    width: Int, height: Int, bytesPerPixel: Int, queueFamily: QueueFamily?,
    storeResult: CompletableDeferred<VulkanImage>, val prefill: (ByteBuffer) -> Unit
): ImageMemoryClaim(width, height, bytesPerPixel, queueFamily, storeResult) {

    constructor(
        width: Int, height: Int, bytesPerPixel: Int, queueFamily: QueueFamily,
        storeResult: CompletableDeferred<VulkanImage>, prefillImage: Supplier<BufferedImage>
    ) : this(width, height, bytesPerPixel, queueFamily, storeResult, { destBuffer ->
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
    )
}

class UninitializedImageMemoryClaim(
    width: Int, height: Int, bytesPerPixel: Int, queueFamily: QueueFamily?, storeResult: CompletableDeferred<VulkanImage>
): ImageMemoryClaim(width, height, bytesPerPixel, queueFamily, storeResult)
