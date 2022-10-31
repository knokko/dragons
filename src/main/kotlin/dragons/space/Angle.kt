package dragons.space

import org.joml.Math.*

@JvmInline
value class Angle private constructor(
    /**
     * The raw angle, in radians
     */
    private val raw: Float
) {

    val radians: Float
    get() = raw

    val degrees: Float
    get() = toDegrees(raw.toDouble()).toFloat()

    val sin: Float
    get() = sin(radians)

    val cos: Float
    get() = cos(radians)

    override fun toString() = "$degrees degrees"

    operator fun plus(other: Angle) = Angle(this.raw + other.raw)

    operator fun minus(right: Angle) = Angle(this.raw - right.raw)

    companion object {
        fun degrees(angle: Float) = Angle(toRadians(angle))

        fun radians(angle: Float) = Angle(angle)
    }
}
