package graviks2d.target

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color

interface GraviksTarget {
    fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color)

    fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference)

    fun drawString(
        minX: Float, yBottom: Float, maxX: Float, yTop: Float,
        string: String, style: TextStyle, backgroundColor: Color,
    )
}
