package dsl.pm2.interpreter.value

class Pm2MatrixIndexValue(internal val index: Int) : Pm2Value() {
    override fun copy() = this

    override fun equals(other: Any?) = other is Pm2MatrixIndexValue && this.index == other.index

    override fun hashCode() = this.index

    override fun toString() = "Pm2MatrixIndex($index)"
}
