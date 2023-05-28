package gruviks.util

import graviks2d.resource.image.ImageReference
import graviks2d.resource.text.CharacterPosition
import graviks2d.resource.text.FontReference
import graviks2d.resource.text.TextStyle
import graviks2d.target.GraviksScissor
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import kotlin.math.abs

import kotlin.math.min
import kotlin.math.max

class LoggedGraviksTarget: GraviksTarget {

    val fillRectCalls = mutableSetOf<FillRectCall>()
    private var currentScissor = GraviksScissor(0f, 0f, 1f, 1f)

    override fun setScissor(newScissor: GraviksScissor): GraviksScissor {
        val oldScissor = currentScissor
        currentScissor = newScissor
        return oldScissor
    }

    override fun getScissor() = currentScissor

    override fun fillTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Color) {
        TODO("Not yet implemented")
    }

    override fun fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) {
        fillRectCalls.add(FillRectCall(
            min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), color, currentScissor
        ))
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
        TODO("Not yet implemented")
    }

    override fun drawImage(xLeft: Float, yBottom: Float, xRight: Float, yTop: Float, image: ImageReference) {
        TODO("Not yet implemented")
    }

    override fun getImageSize(image: ImageReference): Pair<Int, Int> {
        TODO("Not yet implemented")
    }

    override fun drawString(
        minX: Float,
        yBottom: Float,
        maxX: Float,
        yTop: Float,
        string: String,
        style: TextStyle,
        dryRun: Boolean,
        suggestLeftToRight: Boolean
    ): List<CharacterPosition> {
        TODO("Not yet implemented")
    }

    override fun getStringAspectRatio(string: String, fontReference: FontReference?) = 10f

    override fun getAspectRatio() = 1f
}

class FillRectCall(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val color: Color,
    val scissor: GraviksScissor
) {
    override fun equals(other: Any?) = other is FillRectCall && color == other.color && scissor.isSimilar(other.scissor) &&
            abs(minX - other.minX) + abs(minY - other.minY) + abs(maxX - other.maxX) + abs(maxY - other.maxY) < 0.001f

    override fun hashCode(): Int {
        var result = minX.hashCode()
        result = 31 * result + minY.hashCode()
        result = 31 * result + maxX.hashCode()
        result = 31 * result + maxY.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + scissor.hashCode()
        return result
    }

    override fun toString() = "FillRectCall($minX, $minY, $maxX, $maxY, $color, $scissor)"
}
