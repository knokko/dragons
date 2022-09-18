package dragons.space

@JvmInline
value class Distance private constructor(
    /**
     * The raw distance, in nanometers
     */
    private val raw: Long
) {

    val nanoMeters: Float
    get() = raw.toFloat()

    val microMeters: Float
    get() = raw.toFloat() / 1000f

    val milliMeters: Float
    get() = raw.toFloat() / 1000_000f

    val meters: Float
    get() = raw.toFloat() / 1000_000_000f

    val kiloMeters: Float
    get() = raw.toFloat() / 1000_000_000_000f

    val nanoMetersInt: Long
    get() = raw

    val microMetersInt: Long
    get() = nanoMetersInt / 1000

    val milliMetersInt: Long
    get() = microMetersInt / 1000

    val metersInt: Long
    get() = milliMetersInt / 1000

    val kiloMetersInt: Long
    get() = metersInt / 1000

    operator fun plus(other: Distance) = Distance(raw + other.raw)

    operator fun minus(right: Distance) = Distance(raw - right.raw)

    operator fun times(scalar: Float) = Distance((raw.toFloat() * scalar).toLong())

    operator fun times(scalar: Long) = Distance(raw * scalar)

    operator fun div(right: Long) = Distance(raw / right)

    operator fun div(right: Float) = Distance((raw.toFloat() / right).toLong())

    companion object {
        fun nanoMeters(distance: Long) = Distance(distance)

        fun nanoMeters(distance: Float) = Distance(distance.toLong())

        fun microMeters(distance: Long) = nanoMeters(distance * 1000)

        fun microMeters(distance: Float) = Distance((distance * 1000f).toLong())

        fun milliMeters(distance: Long) = microMeters(distance * 1000)

        fun milliMeters(distance: Float) = Distance((distance * 1000_000f).toLong())

        fun meters(distance: Int) = meters(distance.toLong())

        fun meters(distance: Long) = milliMeters(distance * 1000)

        fun meters(distance: Float) = Distance((distance * 1000_000f).toLong() * 1000)

        fun kiloMeters(distance: Long) = meters(distance * 1000)

        fun kiloMeters(distance: Float) = Distance((distance * 1000_000f).toLong() * 1000_000)
    }
}
