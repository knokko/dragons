package dsl.pm2.interpreter.value

import dsl.pm2.interpreter.Pm2RuntimeError

class Pm2IntValue(private val value: Int) : Pm2Value() {

    override fun intValue() = this.value

    override fun plus(right: Pm2Value) = if (right is Pm2IntValue) Pm2IntValue(this.value + right.value) else super.plus(right)

    override fun minus(right: Pm2Value) = if (right is Pm2IntValue) Pm2IntValue(this.value - right.value) else super.minus(right)

    override fun times(right: Pm2Value) = when(right) {
        is Pm2IntValue -> Pm2IntValue(this.value * right.value)
        is Pm2FloatValue -> Pm2FloatValue(this.value * right.floatValue())
        is Pm2ListValue -> right.times(this)
        else -> super.times(right)
    }

    override fun div(right: Pm2Value) = when(right) {
        is Pm2IntValue -> if (right.value != 0) Pm2IntValue(this.value / right.value) else throw Pm2RuntimeError("Division by 0")
        is Pm2FloatValue -> Pm2FloatValue(this.value / right.floatValue())
        else -> super.div(right)
    }

    override fun compareTo(right: Pm2Value): Int {
        return if (right is Pm2IntValue) this.value.compareTo(right.value)
        else super.compareTo(right)
    }

    override fun equals(other: Any?) = other is Pm2IntValue && this.value == other.value

    override fun hashCode() = this.value

    override fun toString() = "IntValue($value)"

    override fun copy() = this
}
