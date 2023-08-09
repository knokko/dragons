package dsl.pm2.interpreter.instruction

import dsl.pm2.interpreter.Pm2Type
import dsl.pm2.interpreter.value.Pm2Value

class Pm2Instruction(
    val type: Pm2InstructionType,
    val lineNumber: Int,
    /**
     * When *type* is `PushValue`, this is the value to be pushed, or null if the default value of *variableType*
     * should be pushed. For all other instructions, this must be null.
     */
    val value: Pm2Value? = null,
    /**
     * When *type* is `DeclareVariable` or `TransferVariable`, this is the type of the variable to be declared.
     * For all other instructions, this must be null.
     */
    val variableType: Pm2Type? = null,
    /**
     * - When *type* is `PushVariable`, `DeclareVariable`, `ReassignVariable`, or `TransferVariable`,
     * this is the name of the variable.
     * - When *type* is `PushProperty` or `SetProperty`, this is the name of the property.
     * - When *type* is `InvokeBuiltinFunction`, this is the name of the function
     * - When *type* is `AssignParameter`, this is the name of the parameter
     * - For all other instructions, this must be null.
     */
    val name: String? = null
) {
    init {
        if (type != Pm2InstructionType.PushValue && value != null) {
            throw IllegalArgumentException("Type $type must not have a value")
        }
        if ((type != Pm2InstructionType.DeclareVariable && type != Pm2InstructionType.TransferVariable) && variableType != null) {
            throw IllegalArgumentException("Type $type must not have a variable type")
        }
        if ((type == Pm2InstructionType.DeclareVariable || type == Pm2InstructionType.TransferVariable) && variableType == null) {
            throw IllegalArgumentException("DeclareVariable and TransferVariable instructions must have a type")
        }

        if ((type == Pm2InstructionType.PushVariable || type == Pm2InstructionType.DeclareVariable
                    || type == Pm2InstructionType.ReassignVariable || type == Pm2InstructionType.PushProperty
                    || type == Pm2InstructionType.SetProperty || type == Pm2InstructionType.InvokeBuiltinFunction
                    || type == Pm2InstructionType.TransferVariable || type == Pm2InstructionType.AssignParameter
                )) {
            if (name == null) throw IllegalArgumentException("Type $type must have a name")
        } else if (name != null) throw IllegalArgumentException("Type $type must not have a name")
    }

    override fun toString(): String {
        var result = type.toString()
        if (value != null) result = "$result $value"
        if (name != null) result = "$result $name"
        return result
    }
}
