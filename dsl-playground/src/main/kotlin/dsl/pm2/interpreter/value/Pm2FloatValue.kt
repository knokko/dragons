package dsl.pm2.interpreter.value

class Pm2FloatValue(private val value: Float) : Pm2Value() {

    override fun floatValue() = value

    override fun div(right: Pm2Value): Pm2Value {
        return when(right) {
            is Pm2FloatValue -> Pm2FloatValue(this.value / right.value)
            is Pm2IntValue -> Pm2FloatValue(this.value / right.intValue())
            else -> super.div(right)
        }
    }

    override fun times(right: Pm2Value): Pm2Value {
        return when(right) {
            is Pm2FloatValue -> Pm2FloatValue(this.value * right.value)
            is Pm2IntValue -> Pm2FloatValue(this.value * right.intValue())
            else -> super.times(right)
        }
    }

    override fun minus(right: Pm2Value): Pm2Value {
        return when(right) {
            is Pm2FloatValue -> Pm2FloatValue(this.value - right.value)
            is Pm2IntValue -> Pm2FloatValue(this.value - right.intValue())
            else -> super.minus(right)
        }
    }

    override fun plus(right: Pm2Value): Pm2Value {
        return when(right) {
            is Pm2FloatValue -> Pm2FloatValue(this.value + right.value)
            is Pm2IntValue -> Pm2FloatValue(this.value + right.intValue())
            else -> super.plus(right)
        }
    }

    override fun toString() = "FloatValue($value)"

    override fun copy() = this
}
