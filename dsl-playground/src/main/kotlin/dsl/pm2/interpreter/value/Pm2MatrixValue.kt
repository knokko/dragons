package dsl.pm2.interpreter.value

import org.joml.Matrix3x2f

class Pm2MatrixValue(internal val matrix: Matrix3x2f): Pm2Value() {
    override fun copy() = Pm2MatrixValue(Matrix3x2f(matrix))
}
