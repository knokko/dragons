package gruviks.space

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
    }
}
