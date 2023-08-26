package dsl.pm2.interpreter.program

import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.*
import kotlin.jvm.Throws

internal class Pm2VertexProcessor(
    private val program: Pm2Program,
    rawStaticParameters: Pm2Value
): Pm2BaseProcessor(program.instructions) {

    private val programStack = mutableListOf<ProgramEntry>()

    private var vertices = mutableListOf<Pm2VertexValue>()
    private var dynamicMatrices = mutableListOf<Pm2DynamicMatrix?>(null)

    private var transferVariables = mutableMapOf<String, Pair<Pm2Type, Pm2Value>>()

    init {
        initializeStaticParameters(rawStaticParameters, program.staticParameters)
    }

    @Throws(Pm2RuntimeError::class)
    fun execute(): Pm2Model {
        executeInstructions()
        variables.popScope() // Pop static parameter values

        if (vertices.isEmpty()) throw Pm2RuntimeError("Not a single triangle was produced")
        if (valueStack.isNotEmpty()) throw IllegalStateException("Value stack should be empty")
        if (variables.hasScope()) throw IllegalStateException("All scopes should have been popped")

        return Pm2Model(vertices.map { it.toVertex() }, dynamicMatrices, program.dynamicParameters)
    }

    private fun initializeStaticParameters(rawStaticParameters: Pm2Value, staticParameters: Map<String, Pm2Type>) {
        val staticParameterValues = mutableMapOf<String, Pm2Value>()
        if (staticParameters.isEmpty()) {
            if (rawStaticParameters is Pm2MapValue) {
                if (rawStaticParameters.map.isNotEmpty()) {
                    throw Pm2RuntimeError("No parameters needed, but ${rawStaticParameters.map.size} were specified")
                }
            } else if (rawStaticParameters !is Pm2NoneValue) {
                throw Pm2RuntimeError("No parameters expected, but got $rawStaticParameters")
            }
        } else {
            if (rawStaticParameters !is Pm2MapValue) {
                throw Pm2RuntimeError("Expected a Map of parameters, but got $rawStaticParameters")
            }
            for (key in rawStaticParameters.map.keys) {
                if (key !is Pm2StringValue) throw Pm2RuntimeError("All static parameter keys should be strings, but found $key")
                if (!staticParameters.containsKey(key.value)) throw Pm2RuntimeError("Unexpected static parameter $key")
            }

            for ((key, expectedType) in staticParameters) {
                val value = rawStaticParameters.map[Pm2StringValue(key)] ?:
                throw Pm2RuntimeError("Missing static parameter $key")
                if (!expectedType.acceptValue(value)) {
                    throw Pm2RuntimeError("Type $expectedType of static parameter $key doesn't accept $value")
                }
                staticParameterValues[key] = value
            }
        }

        variables.pushScope()
        for ((key, value) in staticParameterValues) {
            val type = staticParameters[key] ?: throw Pm2RuntimeError("Unknown static parameter $key")
            variables.defineVariable(type, key, value.copy())
        }
    }

    override fun executeInstruction(instruction: Pm2Instruction) {
        when (instruction.type) {
            Pm2InstructionType.TransferVariable -> transferVariable(instruction)
            Pm2InstructionType.CreateDynamicMatrix -> createDynamicMatrix()

            else -> super.executeInstruction(instruction)
        }
    }

    override fun handleChildModel(instruction: Pm2Instruction, currentInstructionIndex: Int): Int {
        val modelID = instruction.name!!
        val (targetLineNumber, staticParameterTypes, dynamicParameterTypes) = program.childProgramTable[modelID]!!

        val dynamicChildBlockIndex = valueStack.removeLast().castTo<Pm2IntValue>().intValue()
        val staticParameters = valueStack.removeLast()
        val parentMatrix = valueStack.removeLast().castTo<Pm2MatrixIndexValue>()

        programStack.add(ProgramEntry(
            currentInstructionIndex + 1, parentMatrix, dynamicChildBlockIndex, dynamicParameterTypes,
            this.variables, this.vertices, this.dynamicMatrices, this.transferVariables
        ))

        this.variables = Pm2VariableScope()
        this.vertices = mutableListOf()
        this.dynamicMatrices = mutableListOf(null)
        this.transferVariables = mutableMapOf()

        this.initializeStaticParameters(staticParameters, staticParameterTypes)

        return targetLineNumber
    }

    override fun handleExit(): Int {
        val entry = programStack.removeLastOrNull() ?: return -1

        for ((index, childMatrix) in this.dynamicMatrices.withIndex()) {
            if (index == 0) {
                if (childMatrix != null) throw Pm2RuntimeError("First dynamic matrix must be null")
            } else {
                if (childMatrix == null) throw Pm2RuntimeError("Only first dynamic matrix must be null")
                if (childMatrix.parentIndex == 0) childMatrix.parentIndex = entry.parentMatrix.index
                else childMatrix.parentIndex += entry.dynamicMatrices.size - 1
            }
        }

        for (childVertex in this.vertices.toSet()) {
            val childMatrixIndex = childVertex.getProperty("matrix").castTo<Pm2MatrixIndexValue>().index
            if (childMatrixIndex == 0) childVertex.setProperty("matrix", entry.parentMatrix)
            else childVertex.setProperty("matrix", Pm2MatrixIndexValue(entry.dynamicMatrices.size + childMatrixIndex - 1))
        }

        this.variables.popScope()
        if (this.variables.hasScope()) throw Pm2RuntimeError("Unexpected variable scope left")

        this.variables = entry.variables
        entry.vertices.addAll(this.vertices)
        this.vertices = entry.vertices
        entry.dynamicMatrices.addAll(this.dynamicMatrices.subList(1, this.dynamicMatrices.size))
        this.dynamicMatrices = entry.dynamicMatrices
        this.transferVariables = entry.transferVariables

        return entry.returnInstructionIndex
    }

    private fun createDynamicMatrix() {
        val dynamicIndex = valueStack.removeLast().intValue()
        val matrixIndex = dynamicMatrices.size

        val programEntry = programStack.lastOrNull()
        val (propagationInstructions, dynamicParameterTypes) = if (programEntry != null) {
            Pair(program.dynamicChildParameterBlocks[programEntry.dynamicChildBlockIndex], programEntry.dynamicParameters)
        } else Pair(null, program.dynamicParameters)

        dynamicMatrices.add(Pm2DynamicMatrix(
                program.dynamicBlocks[dynamicIndex],
                propagationInstructions,
                dynamicParameterTypes,
                transferVariables
        ))
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

    private class ProgramEntry(
        val returnInstructionIndex: Int,
        val parentMatrix: Pm2MatrixIndexValue,
        val dynamicChildBlockIndex: Int,
        val dynamicParameters: Map<String, Pm2Type>,
        val variables: Pm2VariableScope,
        val vertices: MutableList<Pm2VertexValue>,
        val dynamicMatrices: MutableList<Pm2DynamicMatrix?>,
        val transferVariables: MutableMap<String, Pair<Pm2Type, Pm2Value>>
    )
}
