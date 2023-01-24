package dragons.geometry

@JvmInline
value class Distance internal constructor(val rawValue: CoordinateType): Comparable<Distance> {

    val milliMeters: CoordinateType
        get() = meters * 1000

    val meters: CoordinateType
        get() = rawValue

    operator fun unaryMinus() = Distance(-rawValue)

    operator fun plus(other: Distance) = Distance(rawValue + other.rawValue)

    operator fun plus(other: Coordinate) = Coordinate(rawValue + other.rawValue)

    operator fun minus(right: Distance) = Distance(rawValue - right.rawValue)

    operator fun times(scalar: Int) = Distance(rawValue * scalar)

    operator fun times(scalar: Long) = Distance(rawValue * scalar)

    operator fun times(scalar: Float) = Distance(rawValue * scalar)

    operator fun times(scalar: Double) = Distance(rawValue * scalar)

    operator fun times(scalar: CoordinateType) = Distance(rawValue * scalar)

    operator fun times(vector: Vector) = Vector(vector.x * rawValue, vector.y * rawValue, vector.z * rawValue)

    operator fun times(other: Distance) = Area.squareMeters(this.meters.toDouble() * other.meters.toDouble())

    operator fun div(right: Int) = Distance(rawValue / right)

    operator fun div(right: Long) = Distance(rawValue / right)

    operator fun div(right: Float) = Distance(rawValue / right)

    operator fun div(right: Double) = Distance(rawValue / right)

    operator fun div(right: CoordinateType) = Distance(rawValue / right)

    operator fun div(right: Distance) = rawValue / right.rawValue

    override fun compareTo(other: Distance) = this.rawValue.compareTo(other.rawValue)

    override fun toString() = rawValue.toString() + "m"

    companion object {

        val ZERO = Distance(CoordinateType.ZERO)

        fun milliMeters(distance: Int) = Distance(CoordinateType.fraction(distance, 1000))

        fun milliMeters(distance: Long) = Distance(CoordinateType.fraction(distance, 1000L))

        fun milliMeters(distance: Float) = Distance(CoordinateType.fromFloat(distance) / 1000)

        fun milliMeters(distance: Double) = Distance(CoordinateType.fromDouble(distance) / 1000)

        fun meters(distance: Int) = Distance(CoordinateType.fromInt(distance))

        fun meters(distance: Long) = Distance(CoordinateType.fromLong(distance))

        fun meters(distance: Float) = Distance(CoordinateType.fromFloat(distance))

        fun meters(distance: Double) = Distance(CoordinateType.fromDouble(distance))

        fun kiloMeters(distance: Int) = Distance(CoordinateType.fromInt(distance) * 1000)
    }
}
