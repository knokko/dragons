package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.*
import org.joml.Math.*
import kotlin.jvm.Throws

internal abstract class Pm2BaseProcessor(
        protected val instructions: List<Pm2Instruction>
) {
    protected val variables = Pm2VariableScope()
    protected val valueStack = mutableListOf<Pm2Value>()

    @Throws(Pm2RuntimeError::class)
    protected fun executeInstructions() {
        variables.pushScope()

        var instructionIndex = 0
        while (instructionIndex < instructions.size) {
            val instruction = instructions[instructionIndex]
            instructionIndex += try {
                if (instruction.type == Pm2InstructionType.Jump) {
                    val jumpOffset = valueStack.removeLast().intValue()
                    val shouldJump = valueStack.removeLast().booleanValue()
                    if (shouldJump) jumpOffset else 1
                } else {
                    executeInstruction(instruction)
                    1
                }
            } catch (error: Pm2RuntimeError) {
                throw Pm2RuntimeError(error.message!!, instruction.lineNumber)
            }
        }

        variables.popScope()

    }

    protected open fun executeInstruction(instruction: Pm2Instruction) {
        when (instruction.type) {
            Pm2InstructionType.PushValue -> valueStack.add(instruction.value ?: instruction.variableType!!.createDefaultValue!!.invoke())
            Pm2InstructionType.PushVariable -> valueStack.add(
                    variables.getVariable(instruction.name!!) ?: throw Pm2RuntimeError("Undefined variable ${instruction.name}")
            )
            Pm2InstructionType.PushProperty -> valueStack.add(valueStack.removeLast().getProperty(instruction.name!!))

            Pm2InstructionType.Duplicate -> valueStack.add(valueStack.last())
            Pm2InstructionType.Swap -> {
                val x = valueStack.removeLast()
                val y = valueStack.removeLast()
                valueStack.add(x)
                valueStack.add(y)
            }
            Pm2InstructionType.Delete -> valueStack.removeLast()

            Pm2InstructionType.Divide -> { val right = valueStack.removeLast(); valueStack.add(valueStack.removeLast() / right) }
            Pm2InstructionType.Multiply -> { val right = valueStack.removeLast(); valueStack.add(valueStack.removeLast() * right) }
            Pm2InstructionType.Add -> { val right = valueStack.removeLast(); valueStack.add(valueStack.removeLast() + right) }
            Pm2InstructionType.Subtract -> { val right = valueStack.removeLast(); valueStack.add(valueStack.removeLast() - right) }

            Pm2InstructionType.SmallerThan -> { val right = valueStack.removeLast(); valueStack.add(Pm2BooleanValue(valueStack.removeLast() < right)) }
            Pm2InstructionType.SmallerOrEqual -> { val right = valueStack.removeLast(); valueStack.add(Pm2BooleanValue(valueStack.removeLast() <= right)) }

            Pm2InstructionType.DeclareVariable -> variables.defineVariable(
                    instruction.variableType!!, instruction.name!!, valueStack.removeLast()
            )
            Pm2InstructionType.ReassignVariable -> variables.reassignVariable(instruction.name!!, valueStack.removeLast())
            Pm2InstructionType.SetProperty -> { val newValue = valueStack.removeLast(); valueStack.removeLast().setProperty(instruction.name!!, newValue) }

            Pm2InstructionType.InvokeBuiltinFunction -> invokeBuiltinFunction(instruction.name!!)

            Pm2InstructionType.PushScope -> variables.pushScope()
            Pm2InstructionType.PopScope -> variables.popScope()

            else -> throw Pm2RuntimeError("Unknown instruction type ${instruction.type}")
        }
    }

    protected open fun invokeBuiltinFunction(name: String) {
        when (name) {
            "constructPosition" -> Pm2BuiltinFunction.CONSTRUCT_POSITION.invoke(valueStack) { parameters ->
                Pm2PositionValue(parameters[0].floatValue(), parameters[1].floatValue())
            }
            "rgb" -> Pm2BuiltinFunction.RGB.invoke(valueStack) { parameters ->
                Pm2ColorValue(parameters[0].floatValue(), parameters[1].floatValue(), parameters[2].floatValue())
            }
            "int" -> Pm2BuiltinFunction.INT.invoke(valueStack) { parameters -> Pm2IntValue(parameters[0].floatValue().toInt()) }
            "float" -> Pm2BuiltinFunction.FLOAT.invoke(valueStack) { parameters -> Pm2FloatValue(parameters[0].intValue().toFloat()) }
            "sin" -> Pm2BuiltinFunction.SIN.invoke(valueStack) { parameters -> Pm2FloatValue(sin(toRadians(parameters[0].floatValue()))) }
            "cos" -> Pm2BuiltinFunction.COS.invoke(valueStack) { parameters -> Pm2FloatValue(cos(toRadians(parameters[0].floatValue()))) }
            "translate" -> Pm2BuiltinFunction.TRANSLATE_MATRIX.invoke(valueStack) { parameters ->
                parameters[0].castTo<Pm2MatrixValue>().matrix.translate(parameters[1].floatValue(), parameters[2].floatValue())
                null
            }
            "rotate" -> Pm2BuiltinFunction.ROTATE_MATRIX.invoke(valueStack) { parameters ->
                parameters[0].castTo<Pm2MatrixValue>().matrix.rotate(toRadians(parameters[1].floatValue()))
                null
            }
            "scale" -> Pm2BuiltinFunction.SCALE_MATRIX.invoke(valueStack) { parameters ->
                parameters[0].castTo<Pm2MatrixValue>().matrix.scale(parameters[1].floatValue(), parameters[2].floatValue())
                null
            }
            else -> throw Pm2RuntimeError("Unknown built-in function $name")
        }
    }
}
