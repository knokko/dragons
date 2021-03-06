package graviks2d.target

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color

interface GraviksTarget {
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

    fun drawString(
        minX: Float, yBottom: Float, maxX: Float, yTop: Float,
        string: String, style: TextStyle, backgroundColor: Color,
    )

    fun getStringAspectRatio(string: String, fontReference: FontReference?): Float

    /**
     * Returns the width of this target divided by the height of this target
     */
    fun getAspectRatio(): Float
}
