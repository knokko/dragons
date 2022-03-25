package graviks2d.util

import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class HostImage(val width: Int, val height: Int, val hostBuffer: ByteBuffer, val flipY: Boolean) {

    private fun checkBounds(x: Int, y: Int) {
        if (x < 0) throw IllegalArgumentException("x ($x) must be non-negative")
        if (y < 0) throw IllegalArgumentException("y ($y) must be non-negative")
        if (x >= width) throw IllegalArgumentException("x ($x) must be smaller than $width")
        if (y >= height) throw IllegalArgumentException("y ($y) must be smaller than $height")
    }

    private fun indexFor(x: Int, y: Int): Int {
        checkBounds(x, y)
        return 4 * (x + if (flipY) { height - y - 1} else { y } * width)
    }

    fun getPixel(x: Int, y: Int): Color {
        val index = indexFor(x, y)
        fun toComponent(byteValue: Byte) = byteValue.toUByte().toInt()
        return Color.rgbaInt(
            toComponent(hostBuffer[index]),
            toComponent(hostBuffer[index + 1]),
            toComponent(hostBuffer[index + 2]),
            toComponent(hostBuffer[index + 3])
        )
    }

    fun setPixel(x: Int, y: Int, color: Color) {
        val index = indexFor(x, y)
        hostBuffer.put(index, color.red.toByte())
        hostBuffer.put(index + 1, color.green.toByte())
        hostBuffer.put(index + 2, color.blue.toByte())
        hostBuffer.put(index + 3, color.alpha.toByte())
    }

    fun saveToDisk(dest: File) {
        val bufferedImage = BufferedImage(width, height, TYPE_INT_ARGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = getPixel(x, y)
                bufferedImage.setRGB(x, y, java.awt.Color(pixel.red, pixel.green, pixel.blue, pixel.alpha).rgb)
            }
        }
        ImageIO.write(bufferedImage, "PNG", dest)
    }
}
