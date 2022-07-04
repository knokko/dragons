package graviks2d.target

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color

class ChildTarget(
    private val parent: GraviksTarget,
    private val minX: Float,
    private val minY: Float,
    private val maxX: Float,
    private val maxY: Float
): GraviksTarget {
    private fun transform(x1: Float, y1: Float, x2: Float, y2: Float, drawFunction: (Float, Float, Float, Float) -> Unit) {
        fun transformX(x: Float) = this.minX + x * (this.maxX - this.minX)
        fun transformY(y: Float) = this.minY + y * (this.maxY - this.minY)

        drawFunction(transformX(x1), transformY(y1), transformX(x2), transformY(y2))
    }

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        this.transform(x1, y1, x2, y2) { tx1, ty1, tx2, ty2 ->
            this.parent.fillRect(tx1, ty1, tx2, ty2, color)
        }
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        this.transform(xLeft, yBottom, xRight, yTop) { tLeft, tBottom, tRight, tTop ->
            this.parent.drawImage(tLeft, tBottom, tRight, tTop, image)
        }
    }

    override fun drawString(
        minX: Float,
        yBottom: Float,
        maxX: Float,
        yTop: Float,
        string: String,
        style: TextStyle,
        backgroundColor: Color
    ) {
        this.transform(minX, yBottom, maxX, yTop) { tMinX, tBottom, tMaxX, tTop ->
            this.parent.drawString(tMinX, tBottom, tMaxX, tTop, string, style, backgroundColor)
        }
    }
}
