package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.Pm2DynamicMatrix
import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.value.Pm2MatrixValue
import org.joml.Matrix3x2f

internal class Pm2MatrixProcessor(matrixFunction: Pm2DynamicMatrix): Pm2BaseProcessor(matrixFunction.instructions) {

    private val transferredVariables = matrixFunction.transferredVariables

    @Throws(Pm2RuntimeError::class)
    fun execute(): Matrix3x2f {
        variables.pushScope()
        for ((name, variable) in transferredVariables) {
            val (type, value) = variable
            variables.defineVariable(type, name, value)
        }
        executeInstructions()
        variables.popScope()

        if (variables.hasScope()) throw Pm2RuntimeError("Variable scopes aren't empty")

        if (valueStack.size != 1) throw Pm2RuntimeError("Size of valueStack ($valueStack) is not 1")
        return valueStack.removeLast().castTo<Pm2MatrixValue>().matrix
    }
}
