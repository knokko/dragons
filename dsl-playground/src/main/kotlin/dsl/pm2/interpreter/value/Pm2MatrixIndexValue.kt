package dsl.pm2.interpreter.value

class Pm2MatrixIndexValue(internal val index: Int) : Pm2Value() {
    override fun copy() = this
}
