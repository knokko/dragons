package dsl.pm2.interpreter.value

class Pm2BooleanValue(
    private val value: Boolean
): Pm2Value() {
    override fun copy() = this

    override fun booleanValue() = this.value

    override fun toString() = "BooleanValue($value)"

    override fun equals(other: Any?) = other is Pm2BooleanValue && this.value == other.value

    override fun hashCode() = if (value) 1 else 0
}
