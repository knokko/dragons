package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.Pm2Value

internal class Pm2ParameterProcessor(
        instructions: List<Pm2Instruction>,
        private val parameterValues: MutableMap<String, Pm2Value>
): Pm2BaseProcessor(instructions) {

    override fun executeInstruction(instruction: Pm2Instruction) {
        when (instruction.type) {
            Pm2InstructionType.AssignParameter -> assignParameter(instruction)

            else -> super.executeInstruction(instruction)
        }
    }

    private fun assignParameter(instruction: Pm2Instruction) {
        val value = valueStack.removeLast()
        val parameterName = instruction.name!!
        if (parameterValues.containsKey(parameterName)) throw Pm2RuntimeError("Parameter $parameterName is assigned twice")
        parameterValues[parameterName] = value
    }

    fun execute() {
        executeInstructions()

        if (valueStack.isNotEmpty()) throw IllegalStateException("Value stack should be empty")
        if (variables.hasScope()) throw IllegalStateException("All scopes should have been popped")
    }
}
