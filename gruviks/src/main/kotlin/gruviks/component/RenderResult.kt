package gruviks.component

class RenderResult(
    /**
     * The region where anything has been drawn. Giving a more accurate drawn region typically yields a better user
     * experience and better performance. In particular, this will be used to determine whether the mouse is hovering
     * over the component.
     */
    val drawnRegion: DrawnRegion?,

    /**
     * This variable determines what happens when a cursor event happens inside the rectangular *domain* of the
     * component, but outside the *drawnRegion*. If true, the cursor event will be propagated to the component behind
     * it. If false, the event will be discarded.
     */
    val propagateMissedCursorEvents: Boolean
) {
}

abstract class DrawnRegion(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    abstract fun isInside(x: Float, y: Float): Boolean

    fun isWithinBounds(x: Float, y: Float) = x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY
}

class RectangularDrawnRegion(
    minX: Float, minY: Float, maxX: Float, maxY: Float
): DrawnRegion(minX, minY, maxX, maxY) {
    override fun isInside(x: Float, y: Float) = this.isWithinBounds(x, y)

    override fun toString() = "RectangularDrawnRegion($minX, $minY, $maxX, $maxY)"
}

class RoundedRectangularDrawnRegion(
    minX: Float, minY: Float, maxX: Float, maxY: Float, val radiusX: Float, val radiusY: Float
): DrawnRegion(minX, minY, maxX, maxY) {
    override fun isInside(x: Float, y: Float): Boolean {
        if (y < minY || y > maxY) return false

        var dy = 0.5f * (minY + maxY) - y

        var dx = 0f
        if (x < minX + radiusX) {
            dx = x - (minX + radiusX)
        } else if (x > maxX - radiusX) {
            dx = (maxX - radiusX) - x
        }

        if (dx == 0f) return true

        dx /= radiusX
        dy /= radiusY

        return dx * dx + dy * dy <= 1.0
    }
}

private fun transformBack(value: Float, refMin: Float, refMax: Float) = refMin + value * (refMax - refMin)

private fun transform(value: Float, refMin: Float, refMax: Float) = (value - refMin) / (refMax - refMin)

class TransformedDrawnRegion(
    private val region: DrawnRegion,
    private val refMinX: Float,
    private val refMinY: Float,
    private val refMaxX: Float,
    private val refMaxY: Float
): DrawnRegion(
    transformBack(region.minX, refMinX, refMaxX),
    transformBack(region.minY, refMinY, refMaxY),
    transformBack(region.maxX, refMinX, refMaxX),
    transformBack(region.maxY, refMinY, refMaxY)
) {
    override fun isInside(x: Float, y: Float) = region.isInside(
        transform(x, refMinX, refMaxX),
        transform(y, refMinY, refMaxY)
    )
}

class CompositeDrawnRegion(
    val regions: Collection<DrawnRegion>
): DrawnRegion(
    regions.minOf { it.minX },
    regions.minOf { it.minY },
    regions.maxOf { it.maxX },
    regions.maxOf { it.maxY }
) {
    override fun isInside(x: Float, y: Float) = this.regions.any { it.isInside(x, y) }
}
