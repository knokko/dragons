package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.Pm2Type
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.value.Pm2MapValue
import dsl.pm2.interpreter.value.Pm2StringValue
import dsl.pm2.interpreter.value.Pm2Value

internal class Pm2DynamicParameterProcessor(
        instructions: List<Pm2Instruction>,
        private val dynamicParentParameterValues: Map<String, Pm2Value>,
        private val dynamicParentParameterTypes: Map<String, Pm2Type>
): Pm2BaseProcessor(instructions) {

    val result = mutableMapOf<String, Pm2Value>()

    fun execute() {
        variables.pushScope()
        for ((key, value) in dynamicParentParameterValues) {
            variables.defineVariable(dynamicParentParameterTypes[key]!!, key, value.copy())
        }
        executeInstructions()

        val resultMap = valueStack.removeLast()
        if (resultMap is Pm2MapValue) {
            for ((key, value) in resultMap.map) {
                if (key !is Pm2StringValue) throw Pm2RuntimeError("All dynamic parameter map keys must be strings, but got $key")
                result[key.value] = value
            }
        } else {
            throw Pm2RuntimeError("Expected dynamic parameter map, but got $resultMap")
        }

        if (valueStack.isNotEmpty()) throw IllegalStateException("Value stack should be empty")
        variables.popScope()
        if (variables.hasScope()) throw IllegalStateException("All scopes should have been popped")
    }
}
