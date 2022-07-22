package gruviks.space

private const val RAW_ONE = 1_000_000L

@JvmInline
value class Coordinate private constructor(private val rawValue: Long) {

    operator fun compareTo(other: Coordinate) = this.rawValue.compareTo(other.rawValue)

    override fun toString() = String.format("%.3f", this.rawValue.toDouble() / RAW_ONE.toDouble())

    operator fun plus(other: Coordinate) = Coordinate(this.rawValue + other.rawValue)

    operator fun minus(right: Coordinate) = Coordinate(this.rawValue - right.rawValue)

    operator fun times(scalar: Long): Coordinate {
        if (wouldMultiplicationOverflow(rawValue, scalar)) {
            throw IllegalArgumentException("Multiplying rawValue ($rawValue) with scalar ($scalar) would overflow")
        }
        return Coordinate(rawValue * scalar)
    }

    operator fun div(right: Long) = Coordinate(divideRounded(this.rawValue, right))

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
