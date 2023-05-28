package graviks2d.target

import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.absoluteValue

class GraviksScissor(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    override fun equals(other: Any?) = other is GraviksScissor && minX == other.minX && minY == other.minY
            && maxX == other.maxX && maxY == other.maxY

    override fun hashCode() = minX.hashCode() + 7 * minY.hashCode() + 13 * maxX.hashCode() + 32 * maxY.hashCode()

    override fun toString() = String.format("GraviksScissor(%.3f, %.3f, %.3f, %.3f", minX, minY, maxX, maxY)

    fun isSimilar(other: GraviksScissor) = (minX - other.minX).absoluteValue + (minY - other.minY).absoluteValue +
            (maxX - other.maxX).absoluteValue + (maxY - other.maxY).absoluteValue < 0.001f

    fun combine(other: GraviksScissor) = GraviksScissor(
        max(this.minX, other.minX),
        max(this.minY, other.minY),
        min(this.maxX, other.maxX),
        min(this.maxY, other.maxY)
    )
}
