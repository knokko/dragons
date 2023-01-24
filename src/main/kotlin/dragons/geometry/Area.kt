package dragons.geometry

import kotlin.math.sqrt

@JvmInline
value class Area internal constructor(private val rawValue: Double) {

    val squareMeters: Double
        get() = rawValue

    fun squareRoot() = Distance.meters(sqrt(this.squareMeters))

    operator fun plus(other: Area) = Area(this.rawValue + other.rawValue)

    operator fun minus(other: Area) = Area(this.rawValue - other.rawValue)

    operator fun compareTo(other: Area) = this.rawValue.compareTo(other.rawValue)

    companion object {

        fun squareMeters(value: Int) = Area(value.toDouble())

        fun squareMeters(value: Long) = Area(value.toDouble())

        fun squareMeters(value: Float) = Area(value.toDouble())

        fun squareMeters(value: Double) = Area(value)
    }
}
