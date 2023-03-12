package gruviks.util

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksTarget
import graviks2d.util.Color

class DummyGraviksTarget(
    private val dummyAspectRatio: Float = 1f
): GraviksTarget {

    var fillRectCounter = 0
    var drawRoundedRectCounter = 0
    var drawImageCounter = 0
    var drawStringCounter = 0

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        fillRectCounter += 1
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
        drawRoundedRectCounter += 1
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        drawImageCounter += 1
    }

    override fun getImageSize(image: ImageReference) = Pair(32, 32)

    override fun drawString(
        minX: Float,
        yBottom: Float,
        maxX: Float,
        yTop: Float,
        string: String,
        style: TextStyle,
        backgroundColor: Color
    ): List<CharacterPosition> {
        drawStringCounter += 1
        val numCodepoints = string.codePointCount(0, string.length)
        return (0 until numCodepoints).map { CharacterPosition(
            minX = it.toFloat() / numCodepoints.toFloat(),
            minY = 0f,
            maxX = (it + 1).toFloat() / numCodepoints.toFloat(),
            maxY = 1f
        ) }
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?) = 10f

    override fun getAspectRatio() = dummyAspectRatio
}
