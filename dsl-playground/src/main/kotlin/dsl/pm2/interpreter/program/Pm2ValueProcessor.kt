package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.Pm2BuiltinFunction
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.value.Pm2Value

internal class Pm2ValueProcessor(
        instructions: List<Pm2Instruction>,
): Pm2BaseProcessor(instructions) {

    var result: Pm2Value? = null

    fun execute() {
        executeInstructions()

        if (valueStack.isNotEmpty()) throw IllegalStateException("Value stack should be empty")
        if (variables.hasScope()) throw IllegalStateException("All scopes should have been popped")
    }

    override fun invokeBuiltinFunction(name: String) {
        when (name) {
            "outputValue" -> Pm2BuiltinFunction.OUTPUT_VALUE.invoke(valueStack) { value -> result = value[0]; null }
            else -> super.invokeBuiltinFunction(name)
        }
    }
}
