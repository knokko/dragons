package gruviks.space

private const val RAW_ONE = 1_000_000L

@JvmInline
value class Coordinate private constructor(private val rawValue: Long) {

    operator fun compareTo(other: Coordinate) = this.rawValue.compareTo(other.rawValue)

    override fun toString() = String.format("%.3f", this.rawValue.toDouble() / RAW_ONE.toDouble())

    companion object {
        fun percentage(percentage: Int) = fraction(percentage.toLong(), 100)

        fun fraction(numerator: Long, denominator: Long): Coordinate {
            return if (wouldMultiplicationOverflow(numerator, RAW_ONE)) {
                val rawFraction = divideRounded(numerator, denominator)
                if (wouldMultiplicationOverflow(rawFraction, RAW_ONE)) {
                    throw IllegalArgumentException("Result would overflow: rawFraction is $rawFraction")
                }

                Coordinate(RAW_ONE * rawFraction)
            } else {
                Coordinate(divideRounded(numerator * RAW_ONE, denominator))
            }
        }
    }
}

class Point(
    val x: Coordinate,
    val y: Coordinate
) {
    override fun toString() = "Point($x, $y)"

    companion object {
        fun fraction(x: Long, y: Long, denominator: Long) = Point(
            Coordinate.fraction(x, denominator),
            Coordinate.fraction(y, denominator)
        )

        fun percentage(x: Int, y: Int) = Point(
            Coordinate.percentage(x),
            Coordinate.percentage(y)
        )
    }
}

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
