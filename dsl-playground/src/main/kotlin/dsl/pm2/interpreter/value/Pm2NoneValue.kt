package dsl.pm2.interpreter.value

class Pm2NoneValue : Pm2Value() {
    override fun toString() = "NoneValue"

    override fun copy() = this

    override fun equals(other: Any?) = other is Pm2NoneValue

    override fun hashCode() = 123456
}
