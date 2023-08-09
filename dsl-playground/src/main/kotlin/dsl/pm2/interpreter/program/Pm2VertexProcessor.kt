package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.*
import org.joml.Math.*
import kotlin.jvm.Throws

internal class Pm2VertexProcessor(
        private val program: Pm2Program,
        private val staticParameterValues: Map<String, Pm2Value>
): Pm2BaseProcessor(program.instructions) {

    private val dynamicBlocks = program.dynamicBlocks

    private val vertices = mutableListOf<Pm2VertexValue>()
    private val dynamicMatrices = mutableListOf<Pm2DynamicMatrix?>(null)

    private var transferVariables = mutableMapOf<String, Pair<Pm2Type, Pm2Value>>()

    @Throws(Pm2RuntimeError::class)
    fun execute(): Pm2Model {
        initializeStaticParameters()
        executeInstructions()
        variables.popScope() // Pop static parameter values

        if (vertices.isEmpty()) throw Pm2RuntimeError("Not a single triangle was produced")
        if (valueStack.isNotEmpty()) throw IllegalStateException("Value stack should be empty")
        if (variables.hasScope()) throw IllegalStateException("All scopes should have been popped")

        return Pm2Model(vertices.map { it.toVertex() }, dynamicMatrices)
    }

    private fun initializeStaticParameters() {
        variables.pushScope()
        for ((key, value) in staticParameterValues) {
            val type = program.staticParameters[key] ?: throw Pm2RuntimeError("Unknown static parameter $key")
            if (!type.acceptValue(value)) throw IllegalArgumentException("Type $type doesn't accept static parameter $key: $value")
            variables.defineVariable(type, key, value.copy())
        }
        for (key in program.staticParameters.keys) {
            if (!staticParameterValues.containsKey(key)) throw Pm2RuntimeError("No value given for static parameter $key")
        }
    }

    override fun executeInstruction(instruction: Pm2Instruction) {
        when (instruction.type) {
            Pm2InstructionType.TransferVariable -> transferVariable(instruction)
            Pm2InstructionType.CreateDynamicMatrix -> createDynamicMatrix()

            else -> super.executeInstruction(instruction)
        }
    }

    private fun createDynamicMatrix() {
        val dynamicIndex = valueStack.removeLast().intValue()
        val matrixIndex = dynamicMatrices.size
        dynamicMatrices.add(Pm2DynamicMatrix(dynamicBlocks[dynamicIndex], transferVariables))
        transferVariables = mutableMapOf()
        valueStack.add(Pm2MatrixIndexValue(matrixIndex))
    }

    private fun transferVariable(instruction: Pm2Instruction) {
        if (transferVariables.containsKey(instruction.name!!)) {
            throw Pm2RuntimeError("Duplicate transferred variable ${instruction.name}")
        }

        val variable = variables.getVariable(instruction.name) ?: throw Pm2RuntimeError("Unknown variable ${instruction.name}")
        transferVariables[instruction.name] = Pair(instruction.variableType!!, variable.copy())
    }

    override fun invokeBuiltinFunction(name: String) {
        when (name) {
            "produceTriangle" -> Pm2BuiltinFunction.PRODUCE_TRIANGLE.invoke(valueStack) { parameters ->
                vertices.add(parameters[0].castTo())
                vertices.add(parameters[1].castTo())
                vertices.add(parameters[2].castTo())
                null
            }
            else -> super.invokeBuiltinFunction(name)
        }
    }
}
