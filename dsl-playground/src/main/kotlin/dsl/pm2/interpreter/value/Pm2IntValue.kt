package dsl.pm2.interpreter.value

class Pm2IntValue(private val value: Int) : Pm2Value() {

    override fun intValue() = this.value

    override fun times(right: Pm2Value) = when(right) {
        is Pm2IntValue -> Pm2IntValue(this.value * right.value)
        is Pm2FloatValue -> Pm2FloatValue(this.value * right.floatValue())
        else -> super.times(right)
    }
}
