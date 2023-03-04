package gruviks.space

import gruviks.component.RectangularDrawnRegion

class RectRegion(
    val minX: Coordinate,
    val minY: Coordinate,
    val boundX: Coordinate,
    val boundY: Coordinate
) {
    init {
        if (minX > boundX) throw IllegalArgumentException("minX ($minX) can be at most boundX ($boundX)")
        if (minY > boundY) throw IllegalArgumentException("minY ($minY) can be at most boundY ($boundY)")
    }

    fun overlaps(other: RectRegion) = this.minX < other.boundX && this.minY < other.boundY
            && other.minX < this.boundX && other.minY < this.boundY

    override fun toString() = "RectRegion($minX, $minY, $boundX, $boundY)"

    override fun equals(other: Any?) = other is RectRegion && this.minX == other.minX && this.minY == other.minY &&
            this.boundX == other.boundX && this.boundY == other.boundY

    // TODO Test precision of this
    fun transform(x: Float, y: Float) = Pair(
        minX.toFloat() + x * (boundX - minX).toFloat(),
        minY.toFloat() + y * (boundY - minY).toFloat()
    )

    fun transform(point: Point) = transform(point.x.toFloat(), point.y.toFloat())

    fun transform(child: RectRegion): RectangularDrawnRegion {
        val (transformedMinX, transformedMinY) = this.transform(Point(child.minX, child.minY))
        val (transformedBoundX, transformedBoundY) = this.transform(Point(child.boundX, child.boundY))
        return RectangularDrawnRegion(
            transformedMinX, transformedMinY, transformedBoundX, transformedBoundY
        )
    }

    fun transformBack(x: Float, y: Float) = Pair(
        (x - minX.toFloat()) / (boundX - minX).toFloat(),
        (y - minY.toFloat()) / (boundY - minY).toFloat()
    )

    fun transformBack(point: Point) = Pair(
        (point.x - minX).toFloat() / (boundX - minX).toFloat(),
        (point.y - minY).toFloat() / (boundY - minY).toFloat()
    )

    fun transformBack(child: RectRegion): RectangularDrawnRegion {
        val (transformedMinX, transformedMinY) = this.transformBack(Point(child.minX, child.minY))
        val (transformedBoundX, transformedBoundY) = this.transformBack(Point(child.boundX, child.boundY))
        return RectangularDrawnRegion(
            transformedMinX, transformedMinY, transformedBoundX, transformedBoundY
        )
    }

    override fun hashCode(): Int {
        var result = minX.hashCode()
        result = 31 * result + minY.hashCode()
        result = 31 * result + boundX.hashCode()
        result = 31 * result + boundY.hashCode()
        return result
    }

    companion object {
        fun percentage(minX: Int, minY: Int, boundX: Int, boundY: Int) = RectRegion(
            Coordinate.percentage(minX),
            Coordinate.percentage(minY),
            Coordinate.percentage(boundX),
            Coordinate.percentage(boundY)
        )

        fun fraction(minX: Long, minY: Long, boundX: Long, boundY: Long, denominator: Long) = RectRegion(
            Coordinate.fraction(minX, denominator),
            Coordinate.fraction(minY, denominator),
            Coordinate.fraction(boundX, denominator),
            Coordinate.fraction(boundY, denominator)
        )

        fun fromFloat(minX: Float, minY: Float, maxX: Float, maxY: Float) = RectRegion(
            Coordinate.fromFloat(minX),
            Coordinate.fromFloat(minY),
            Coordinate.fromFloat(maxX),
            Coordinate.fromFloat(maxY)
        )
    }
}
