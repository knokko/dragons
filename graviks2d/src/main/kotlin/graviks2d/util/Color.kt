package graviks2d.util

@JvmInline
value class Color private constructor(private val rawValue: Int) {

    val red: Int
    get() = rawValue and 255

    val green: Int
    get() = (rawValue shr 8) and 255

    val blue: Int
    get() = (rawValue shr 16) and 255

    val alpha: Int
    get() = (rawValue shr 24) and 255

    val redF: Float
    get() = red.toFloat() / 255f

    val greenF: Float
    get() = green.toFloat() / 255f

    val blueF: Float
    get() = blue.toFloat() / 255f

    val alphaF: Float
    get() = alpha.toFloat() / 255f

    companion object {
        fun rgbaInt(red: Int, green: Int, blue: Int, alpha: Int): Color {
            fun rangeCheck(value: Int, description: String) {
                if (value < 0) throw IllegalArgumentException("$description ($value) can't be negative")
                if (value > 255) throw IllegalArgumentException("$description ($value) can be at most 255")
            }
            rangeCheck(red, "red")
            rangeCheck(green, "green")
            rangeCheck(blue, "blue")
            rangeCheck(alpha, "alpha")

            val rawValue = (red) or (green shl 8) or (blue shl 16) or (alpha shl 24)
            return Color(rawValue)
        }

        fun rgbaFloat(red: Float, green: Float, blue: Float, alpha: Float): Color {
            fun convert(value: Float) = (value * 255f).toInt()

            return rgbaInt(convert(red), convert(green), convert(blue), convert(alpha))
        }

        fun rgbInt(red: Int, green: Int, blue: Int) = rgbaInt(red, green, blue, 255)

        fun rgbFloat(red: Float, green: Float, blue: Float) = rgbaFloat(red, green, blue, 1f)
    }
}
