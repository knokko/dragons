package dragons.vulkan.memory.claim

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.function.Consumer
import java.util.function.Supplier

class PrefilledImageMemoryClaim(
    val width: Int, val height: Int, val bytesPerPixel: Int, val prefill: Consumer<ByteBuffer>) {

    constructor(width: Int, height: Int, bytesPerPixel: Int, prefillImage: Supplier<BufferedImage>) : this(
        width, height, bytesPerPixel, { destBuffer ->
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

class UninitializedImageMemoryClaim(val width: Int, val height: Int, val bytesPerPixel: Int)
