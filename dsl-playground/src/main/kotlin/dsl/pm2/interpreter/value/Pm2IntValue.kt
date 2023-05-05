package dsl.pm2.interpreter.value

class Pm2IntValue(private val value: Int) : Pm2Value() {

    override fun intValue() = this.value

    override fun plus(right: Pm2Value) = if (right is Pm2IntValue) Pm2IntValue(this.value + right.value) else super.plus(right)

    override fun times(right: Pm2Value) = when(right) {
        is Pm2IntValue -> Pm2IntValue(this.value * right.value)
        is Pm2FloatValue -> Pm2FloatValue(this.value * right.floatValue())
        else -> super.times(right)
    }

    override fun compareTo(right: Pm2Value): Int {
        return if (right is Pm2IntValue) this.value.compareTo(right.value)
        else super.compareTo(right)
    }

    override fun toString() = "IntValue($value)"
}
