package dsl.pm2.interpreter.value

class Pm2BooleanValue(
    private val value: Boolean
): Pm2Value() {
    override fun copy() = this

    override fun booleanValue() = this.value

    override fun toString() = "BooleanValue($value)"
}
