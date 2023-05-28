package graviks2d.target

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color

interface GraviksTarget {
    fun setScissor(newScissor: GraviksScissor): GraviksScissor

    fun getScissor(): GraviksScissor

    fun fillTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Color)

    fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color)

    fun drawRoundedRect(
        x1: Float, y1: Float, x2: Float, y2: Float, radiusX: Float, lineWidth: Float, color: Color
    )

    fun fillRoundedRect(
        x1: Float, y1: Float, x2: Float, y2: Float, radiusX: Float, color: Color
    ) {
        drawRoundedRect(x1, y1, x2, y2, radiusX, 0f, color)
    }

    fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference)

    fun getImageSize(image: ImageReference): Pair<Int, Int>

    @Throws(UnsupportedOperationException::class)
    fun drawVulkanImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, vkImage: Long, vkImageView: Long) {
        throw UnsupportedOperationException("This implementation (${this::class.java.simpleName}) doesn't support this")
    }

    @Throws(UnsupportedOperationException::class)
    fun addWaitSemaphore(vkSemaphore: Long) {
        throw UnsupportedOperationException("This implementation (${this::class.java.simpleName}) doesn't support this")
    }

    fun drawString(
        minX: Float, yBottom: Float, maxX: Float, yTop: Float,
        string: String, style: TextStyle, dryRun: Boolean = false,
        suggestLeftToRight: Boolean = true
    ): List<CharacterPosition>

    fun getStringAspectRatio(string: String, fontReference: FontReference?): Float

    /**
     * Returns the width of this target divided by the height of this target
     */
    fun getAspectRatio(): Float
}
