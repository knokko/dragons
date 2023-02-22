package gruviks.space

class Point(
    val x: Coordinate,
    val y: Coordinate
) {
    override fun toString() = "Point($x, $y)"

    fun toRectRegion() = RectRegion(x, y, x + Coordinate.SMALLEST_POSITIVE_VALUE, y + Coordinate.SMALLEST_POSITIVE_VALUE)

    companion object {
        fun fraction(x: Long, y: Long, denominator: Long) = Point(
            Coordinate.fraction(x, denominator),
            Coordinate.fraction(y, denominator)
        )

        fun percentage(x: Int, y: Int) = Point(
            Coordinate.percentage(x),
            Coordinate.percentage(y)
        )

        fun fromFloat(x: Float, y: Float) = Point(
            Coordinate.fromFloat(x),
            Coordinate.fromFloat(y)
        )
    }
}
