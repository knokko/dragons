package dragons.geometry

import fixie.FixNano64

typealias CoordinateType = FixNano64

@JvmInline
value class Coordinate internal constructor(val rawValue: CoordinateType) : Comparable<Coordinate> {

    val milliMeters: CoordinateType
        get() = meters * 1000

    val meters: CoordinateType
        get() = rawValue

    operator fun unaryMinus() = Coordinate(-this.rawValue)

    operator fun plus(other: Distance) = Coordinate(this.rawValue + other.rawValue)

    operator fun minus(other: Coordinate) = Distance(this.rawValue - other.rawValue)

    operator fun minus(other: Distance) = Coordinate(this.rawValue - other.rawValue)

    override operator fun compareTo(other: Coordinate) = this.rawValue.compareTo(other.rawValue)

    override fun toString() = meters.toString() + "m"

    companion object {

        fun milliMeters(value: Int) = Coordinate(CoordinateType.fraction(value, 1000))

        fun milliMeters(value: Long) = Coordinate(CoordinateType.fraction(value, 1000L))

        fun milliMeters(value: Float) = Coordinate(CoordinateType.fromFloat(value) / 1000)

        fun milliMeters(value: Double) = Coordinate(CoordinateType.fromDouble(value) / 1000)

        fun meters(value: Int) = Coordinate(CoordinateType.fromInt(value))

        fun meters(value: Long) = Coordinate(CoordinateType.fromLong(value))

        fun meters(value: Float) = Coordinate(CoordinateType.fromFloat(value))

        fun meters(value: Double) = Coordinate(CoordinateType.fromDouble(value))

        fun kiloMeters(value: Int) = Coordinate(CoordinateType.fromInt(value) * 1000)
    }
}
