package dsl.pm2.interpreter.value

class Pm2PositionValue(private val x: Float, private val y: Float) : Pm2Value() {

    override fun setProperty(propertyName: String, newValue: Pm2Value) {
        throw UnsupportedOperationException("Positions are immutable")
    }

    override fun getProperty(propertyName: String): Pm2Value {
        return when(propertyName) {
            "x" -> Pm2FloatValue(x)
            "y" -> Pm2FloatValue(y)
            else -> throw UnsupportedOperationException("Unknown property: position.$propertyName")
        }
    }

    override fun toString() = "PositionValue($x, $y)"
}
