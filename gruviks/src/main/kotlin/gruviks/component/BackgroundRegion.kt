package gruviks.component

import kotlin.math.absoluteValue

class BackgroundRegion(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    private fun isClose(x1: Float, x2: Float) = (x2 - x1).absoluteValue < 0.001f

    override fun equals(other: Any?) = other is BackgroundRegion && isClose(this.minX, other.minX)
            && isClose(this.minY, other.minY) && isClose(this.maxX, other.maxX) && isClose(this.maxY, other.maxY)

    override fun toString() = "BackgroundRegion($minX, $minY, $maxX, $maxY)"

    override fun hashCode(): Int {
        var result = minX.hashCode()
        result = 31 * result + minY.hashCode()
        result = 31 * result + maxX.hashCode()
        result = 31 * result + maxY.hashCode()
        return result
    }

}