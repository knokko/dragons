package dsl.pm2.interpreter.value

class Pm2NoneValue : Pm2Value() {
    override fun toString() = "NoneValue"

    override fun copy() = this
}
