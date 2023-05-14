package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.Pm2BuiltinFunction
import dsl.pm2.interpreter.Pm2VariableScope
import dsl.pm2.interpreter.Pm2Vertex
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.*
import org.joml.Math.*

internal class Pm2Processor {
    private val variables = Pm2VariableScope()
    private val valueStack = mutableListOf<Pm2Value>()

    private val vertices = mutableListOf<Pm2VertexValue>()

    fun execute(program: Pm2Program): List<Pm2Vertex> {
        variables.pushScope()

        var instructionIndex = 0
        while (instructionIndex < program.instructions.size) {
            val instruction = program.instructions[instructionIndex]
            if (instruction.type == Pm2InstructionType.Jump) {
                instructionIndex += if (valueStack.removeLast().booleanValue()) instruction.jumpOffset!! else 1
            } else {
                executeInstruction(instruction)
                instructionIndex += 1
            }
        }

        variables.popScope()

        return vertices.map { it.toVertex() }
    }

    private fun executeInstruction(instruction: Pm2Instruction) {
        when (instruction.type) {
            Pm2InstructionType.PushValue -> valueStack.add(instruction.value ?: instruction.variableType!!.createDefaultValue!!.invoke())
            Pm2InstructionType.PushVariable -> valueStack.add(variables.getVariable(instruction.name!!)!!)
            Pm2InstructionType.PushProperty -> valueStack.add(valueStack.removeLast().getProperty(instruction.name!!))

            Pm2InstructionType.Duplicate -> valueStack.add(valueStack.last())
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

            else -> throw RuntimeException("Unknown instruction type ${instruction.type}")
        }
    }

    private fun invokeBuiltinFunction(name: String) {
        when (name) {
            "produceTriangle" -> Pm2BuiltinFunction.PRODUCE_TRIANGLE.invoke(valueStack) { parameters ->
                vertices.add(parameters[0] as Pm2VertexValue)
                vertices.add(parameters[1] as Pm2VertexValue)
                vertices.add(parameters[2] as Pm2VertexValue)
                null
            }
            "constructPosition" -> Pm2BuiltinFunction.CONSTRUCT_POSITION.invoke(valueStack) { parameters ->
                Pm2PositionValue(parameters[0].floatValue(), parameters[1].floatValue())
            }
            "int" -> Pm2BuiltinFunction.INT.invoke(valueStack) { parameters -> Pm2IntValue(parameters[0].floatValue().toInt()) }
            "float" -> Pm2BuiltinFunction.FLOAT.invoke(valueStack) { parameters -> Pm2FloatValue(parameters[0].intValue().toFloat()) }
            "sin" -> Pm2BuiltinFunction.SIN.invoke(valueStack) { parameters -> Pm2FloatValue(sin(toRadians(parameters[0].floatValue()))) }
            "cos" -> Pm2BuiltinFunction.COS.invoke(valueStack) { parameters -> Pm2FloatValue(cos(toRadians(parameters[0].floatValue()))) }
            else -> throw RuntimeException("Unknown built-in function $name")
        }
    }
}
