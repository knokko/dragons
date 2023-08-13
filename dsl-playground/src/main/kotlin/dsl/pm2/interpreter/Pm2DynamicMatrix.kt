package dsl.pm2.interpreter

import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.value.Pm2Value

class Pm2DynamicMatrix(
    val instructions: List<Pm2Instruction>,
    val transferredVariables: Map<String, Pair<Pm2Type, Pm2Value>>
) {
    var parentIndex = 0

    override fun toString() = "DynamicMatrix(${instructions.size} instructions, parent=$parentIndex, transfers $transferredVariables)"
}
