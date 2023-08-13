package dsl.pm2.interpreter.value

class Pm2StringValue(internal val value: String): Pm2Value() {

    override fun copy() = this

    override fun compareTo(right: Pm2Value): Int {
        return if (right is Pm2StringValue) this.value.compareTo(right.value)
        else super.compareTo(right)
    }

    override fun equals(other: Any?) = other is Pm2StringValue && this.value == other.value

    override fun hashCode() = this.value.hashCode()

    override fun toString() = "Pm2StringValue($value)"
}
