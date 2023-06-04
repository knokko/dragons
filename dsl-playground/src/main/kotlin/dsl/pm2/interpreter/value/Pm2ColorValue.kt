package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2RuntimeError

class Pm2ColorValue(private val red: Float, private val green: Float, private val blue: Float) : Pm2Value() {

    init {
        if (red < 0f || red > 1f) throw Pm2RuntimeError("Red ($red) must be in range [0, 1]")
        if (green < 0f || green > 1f) throw Pm2RuntimeError("Green ($green) must be in range [0, 1]")
        if (blue < 0f || blue > 1f) throw Pm2RuntimeError("Blue ($blue) must be in range [0, 1]")
    }

    override fun setProperty(propertyName: String, newValue: Pm2Value) {
        throw Pm2RuntimeError("Colors are immutable")
    }

    override fun getProperty(propertyName: String): Pm2Value {
        return when(propertyName) {
            "red" -> Pm2FloatValue(red)
            "green" -> Pm2FloatValue(green)
            "blue" -> Pm2FloatValue(blue)
            else -> throw Pm2RuntimeError("Unknown property: color.$propertyName")
        }
    }

    override fun toString() = "rgb($red, $green, $blue)"

    override fun copy() = this
}
