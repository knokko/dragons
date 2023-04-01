package graviks2d.target

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.FontReference
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

    private fun transformBack(x1: Float, y1: Float, x2: Float, y2: Float, backFunction: (Float, Float, Float, Float) -> Unit) {
        fun transformX(x: Float) = (x - this.minX) / (this.maxX - this.minX)
        fun transformY(y: Float) = (y - this.minY) / (this.maxY - this.minY)

        backFunction(transformX(x1), transformY(y1), transformX(x2), transformY(y2))
    }

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        this.transform(x1, y1, x2, y2) { tx1, ty1, tx2, ty2 ->
            this.parent.fillRect(tx1, ty1, tx2, ty2, color)
        }
    }

    override fun drawRoundedRect(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        radiusX: Float,
        lineWidth: Float,
        color: Color
    ) {
        val dx = this.maxX - this.minX
        this.transform(x1, y1, x2, y2) { tx1, ty1, tx2, ty2 ->
            this.parent.drawRoundedRect(tx1, ty1, tx2, ty2, radiusX * dx,  lineWidth, color)
        }
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        this.transform(xLeft, yBottom, xRight, yTop) { tLeft, tBottom, tRight, tTop ->
            this.parent.drawImage(tLeft, tBottom, tRight, tTop, image)
        }
    }

    override fun getImageSize(image: ImageReference) = parent.getImageSize(image)

    override fun drawString(
        minX: Float,
        yBottom: Float,
        maxX: Float,
        yTop: Float,
        string: String,
        style: TextStyle,
        dryRun: Boolean
    ): List<CharacterPosition> {
        val result = mutableListOf<CharacterPosition>()
        this.transform(minX, yBottom, maxX, yTop) { tMinX, tBottom, tMaxX, tTop ->
            val childResult = this.parent.drawString(tMinX, tBottom, tMaxX, tTop, string, style, dryRun)
            for (childArea in childResult) {
                transformBack(childArea.minX, childArea.minY, childArea.maxX, childArea.maxY) { bMinX, bMinY, bMaxX, bMaxY ->
                    result.add(CharacterPosition(bMinX, bMinY, bMaxX, bMaxY, childArea.isLeftToRight))
                }
            }
        }
        return result
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?) = parent.getStringAspectRatio(string, fontReference)

    override fun getAspectRatio() = parent.getAspectRatio() * (maxX - minX) / (maxY - minY)
}
