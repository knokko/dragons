package dsl.pm2.interpreter

import dsl.pm2.ProcModel2BaseListener
import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.importer.Pm2Importer
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.interpreter.value.Pm2BooleanValue
import dsl.pm2.interpreter.value.Pm2IntValue
import dsl.pm2.interpreter.value.Pm2ListValue
import dsl.pm2.interpreter.value.Pm2NoneValue
import org.antlr.v4.runtime.tree.ErrorNode
import java.io.File
import java.io.PrintWriter

internal class Pm2Converter(val importer: Pm2Importer, val isChild: Boolean) : ProcModel2BaseListener() {

    private val baseInstructions = mutableListOf<Pm2Instruction>()
    private var instructions = baseInstructions
    private val dynamicDeclarations = mutableListOf<MutableList<Pm2Instruction>>()
    private var isInsideDynamicDeclaration = false
    private val staticParameters = mutableMapOf<String, Pm2Type>()
    private val types = Pm2Types()
    private val functions = Pm2Functions()
    private val importedModelIDs = mutableMapOf<String, String>()

    private val loopIndexStack = mutableListOf<Int>()
    private val functionIndexStack = mutableListOf<Int>()

    lateinit var program: Pm2Program

    override fun visitErrorNode(node: ErrorNode?) {
        println("Encountered error $node")
    }

    override fun enterStart(ctx: ProcModel2Parser.StartContext?) {
        types.pushScope()
        types.defineType("void", BuiltinTypes.VOID)
        types.defineType("float", BuiltinTypes.FLOAT)
        types.defineType("int", BuiltinTypes.INT)
        types.defineType("string", BuiltinTypes.STRING)
        types.defineType("position", BuiltinTypes.POSITION)
        types.defineType("color", BuiltinTypes.COLOR)
        types.defineType("matrix", BuiltinTypes.MATRIX_INDEX)
        types.defineType("Vertex", BuiltinTypes.VERTEX)
        types.defineType("List", BuiltinTypes.LIST)
        types.defineType("Map", BuiltinTypes.MAP)
        types.defineType("Random", BuiltinTypes.RANDOM)
        types.defineType("Matrix", BuiltinTypes.MATRIX)

        functions.pushScope()
    }

    override fun exitStart(ctx: ProcModel2Parser.StartContext?) {
        types.popScope()
        functions.popScope()

        instructions.add(Pm2Instruction(Pm2InstructionType.ExitProgram, lineNumber = ctx!!.stop.line))

        val childProgramTable = mutableMapOf<String, Pair<Int, Map<String, Pm2Type>>>()
        if (!isChild) {
            for (childProgram in importer.cache.models.values) {
                childProgramTable[childProgram.id] = Pair(this.instructions.size, childProgram.program.staticParameters)
                for (instruction in childProgram.program.instructions) {
                    if (instruction.type == Pm2InstructionType.CreateDynamicMatrix) {
                        val indexInstruction = instructions.removeLast()
                        val oldIndex = indexInstruction.value!!.intValue()
                        val newIndex = Pm2IntValue(oldIndex + dynamicDeclarations.size)
                        instructions.add(Pm2Instruction(
                            Pm2InstructionType.PushValue, value = newIndex, lineNumber = indexInstruction.lineNumber
                        ))
                    }
                    instructions.add(instruction)
                }
                this.dynamicDeclarations.addAll(childProgram.program.dynamicBlocks.map { it.toMutableList() })
            }
            val instructionDump = PrintWriter(File("instructions.txt"))
            for ((index, instruction) in instructions.withIndex()) {
                instructionDump.println(String.format("%d %s", index, instruction))
            }
            instructionDump.println("---- DYNAMIC DECLARATIONS ----")
            for ((blockIndex, instructions) in this.dynamicDeclarations.withIndex()) {
                for ((index, instruction) in instructions.withIndex()) {
                    instructionDump.println(String.format("%d %d %s", blockIndex, index, instruction))
                }
                instructionDump.println("-----------------------------------------")
            }
            instructionDump.flush()
            instructionDump.close()
        }

        program = Pm2Program(instructions.toList(), dynamicDeclarations.map { it.toList() }, staticParameters.toMap())
        if (!isChild) program.childProgramTable = childProgramTable
    }

    override fun enterDynamicDeclaration(ctx: ProcModel2Parser.DynamicDeclarationContext?) {
        val identifiers = ctx!!.IDENTIFIER()
        val typeName = identifiers[0].text
        if (typeName != "Matrix") throw Pm2CompileError("Matrix is currently the only dynamic type")

        if (isInsideDynamicDeclaration) throw Pm2CompileError("Nested dynamic declarations are forbidden")
        isInsideDynamicDeclaration = true

        if (identifiers.size % 2 == 0) throw Pm2CompileError("Missing name or type for transfer variable ${identifiers.last().text}")

        for (index in 1 until identifiers.size step 2) {
            val transferTypeName = identifiers[index].text
            val transferVariableName = identifiers[index + 1].text
            val transferVariableType = types.getType(transferTypeName)
            instructions.add(Pm2Instruction(
                    Pm2InstructionType.TransferVariable, lineNumber = ctx.start.line,
                    name = transferVariableName, variableType = transferVariableType
            ))
        }

        val dynamicIndex = dynamicDeclarations.size
        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2IntValue(dynamicIndex)))
        instructions.add(Pm2Instruction(Pm2InstructionType.CreateDynamicMatrix, lineNumber = ctx.start.line))

        val dynamicDeclaration = mutableListOf<Pm2Instruction>()
        dynamicDeclarations.add(dynamicDeclaration)
        instructions = dynamicDeclaration
    }

    override fun exitDynamicDeclaration(ctx: ProcModel2Parser.DynamicDeclarationContext?) {
        if (!isInsideDynamicDeclaration) throw Pm2CompileError("There is no dynamic declaration to exit")
        instructions.add(Pm2Instruction(Pm2InstructionType.ExitProgram, lineNumber = ctx!!.stop.line))
        isInsideDynamicDeclaration = false

        instructions = baseInstructions
    }

    override fun enterFunctionDeclaration(ctx: ProcModel2Parser.FunctionDeclarationContext?) {

        // This ensures that the program will 'jump over the function declaration'
        // it prevents the function from being invoked when it is declared
        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx!!.start.line, value = Pm2BooleanValue(true)))
        functionIndexStack.add(instructions.size)
        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2IntValue(-1)))
        instructions.add(Pm2Instruction(Pm2InstructionType.Jump, lineNumber = ctx.start.line))

        // And store the actual function
        val startInstruction = instructions.size

        // This ensures that all parameters are defined
        val parameters = (2 until ctx.IDENTIFIER().size step 2).map { rawIndex ->
            val parameterType = types.getType(ctx.IDENTIFIER(rawIndex).text)
            val parameterName = ctx.IDENTIFIER(rawIndex + 1).text
            Pair(parameterType, parameterName)
        }

        instructions.add(Pm2Instruction(Pm2InstructionType.PushScope, lineNumber = ctx.start.line))
        for ((parameterType, parameterName) in parameters.reversed()) {
            instructions.add(Pm2Instruction(
                Pm2InstructionType.DeclareVariable,
                lineNumber = ctx.start.line,
                variableType = parameterType,
                name = parameterName
            ))
        }

        val returnType = types.getType(ctx.IDENTIFIER(0).text)
        val function = Pm2Function(startInstruction, returnType)

        val name = ctx.IDENTIFIER(1).text
        val signature = Pm2FunctionSignature(name, parameters.size)

        functions.defineUserFunction(signature, function)
    }

    override fun exitFunctionDeclaration(ctx: ProcModel2Parser.FunctionDeclarationContext?) {

        // The result is implicitly none when no result expression is given
        if (ctx!!.expression() == null) {
            instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.stop.line, value = Pm2NoneValue()))
        }

        // These two instructions are basically a type check
        instructions.add(Pm2Instruction(
            Pm2InstructionType.DeclareVariable,
            lineNumber = ctx.stop.line,
            variableType = types.getType(ctx.IDENTIFIER(0).text),
            name = "\$result"
        ))
        instructions.add(Pm2Instruction(Pm2InstructionType.PushVariable, lineNumber = ctx.stop.line, name = "\$result"))

        // Swap the result with the return address
        instructions.add(Pm2Instruction(Pm2InstructionType.Swap, lineNumber = ctx.stop.line))

        val returnInstructionAddress = instructions.size + 5
        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.stop.line, value = Pm2IntValue(returnInstructionAddress)))

        // Jump offset = return address - address of return jump
        instructions.add(Pm2Instruction(Pm2InstructionType.Subtract, lineNumber = ctx.stop.line))

        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.stop.line, value = Pm2BooleanValue(true)))
        instructions.add(Pm2Instruction(Pm2InstructionType.Swap, lineNumber = ctx.stop.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PopScope, lineNumber = ctx.stop.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.Jump, lineNumber = ctx.stop.line))

        // Finish the 'function declaration jump' that was created during enterFunctionDeclaration
        val jumpOverIndex = functionIndexStack.removeLast()
        val targetIndex = instructions.size
        instructions[jumpOverIndex] = Pm2Instruction(
            Pm2InstructionType.PushValue, lineNumber = ctx.start.line,
            value = Pm2IntValue(targetIndex - jumpOverIndex - 1)
        )
    }

    override fun exitParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        if (ctx!!.PARAMETER_TYPE().text == "static") {
            val typeName = ctx.IDENTIFIER(0).text
            val type = types.getType(typeName)
            val name = ctx.IDENTIFIER(1).text
            if (staticParameters.containsKey(name)) throw Pm2CompileError("Duplicate static parameter $name")
            staticParameters[name] = type
        } else if (ctx.PARAMETER_TYPE().text == "dynamic") {
            TODO("Not yet implemented")
        } else {
            throw Pm2CompileError("Unknown parameter type " + ctx.PARAMETER_TYPE().text)
        }
    }

    override fun enterFunctionInvocation(ctx: ProcModel2Parser.FunctionInvocationContext?) {
        val functionName = ctx!!.IDENTIFIER().text
        val builtinFunction = Pm2BuiltinFunction.MAP[functionName]
        if (builtinFunction == null) {

            // I need to remember the return address via this instruction, but I don't know its value, yet
            // I will supply it during exitFunctionInvocation()
            functionIndexStack.add(instructions.size)
            instructions.add(Pm2Instruction(
                Pm2InstructionType.PushValue,
                lineNumber = ctx.start.line,
                value = Pm2IntValue(-1)
            ))
        }
    }

    override fun exitFunctionInvocation(ctx: ProcModel2Parser.FunctionInvocationContext?) {
        val functionName = ctx!!.IDENTIFIER().text
        val builtinFunction = Pm2BuiltinFunction.MAP[functionName]
        if (builtinFunction != null) {
            if (ctx.expression().size != builtinFunction.parameterTypes.size) {
                throw Pm2CompileError(
                    "Built-in function $functionName requires ${builtinFunction.parameterTypes.size} parameters, but got ${ctx.expression().size}",
                    ctx.start.line, ctx.start.charPositionInLine
                )
            }

            instructions.add(Pm2Instruction(Pm2InstructionType.InvokeBuiltinFunction, lineNumber = ctx.start.line, name = functionName))
        } else {
            val functionSignature = Pm2FunctionSignature(functionName, ctx.expression().size)
            val function = functions.getUserFunction(functionSignature)

            // Jump to the function
            instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2BooleanValue(true)))

            val jumpInstructionIndex = instructions.size + 1
            instructions.add(Pm2Instruction(
                Pm2InstructionType.PushValue,
                lineNumber = ctx.start.line,
                value = Pm2IntValue(function.startInstruction - jumpInstructionIndex)
            ))
            instructions.add(Pm2Instruction(Pm2InstructionType.Jump, lineNumber = ctx.start.line))

            // Fix the 'remember return address' instruction from enterFunctionInvocation()...
            instructions[functionIndexStack.removeLast()] = Pm2Instruction(
                Pm2InstructionType.PushValue,
                lineNumber = ctx.start.line,
                value = Pm2IntValue(jumpInstructionIndex + 1)
            )
        }
    }

    override fun exitImportValue(ctx: ProcModel2Parser.ImportValueContext?) {
        val isRelative = ctx!!.importPath().relativeImportPrefix() != null
        val relativePath = "/" + ctx.importPath().relativeImportPath().text
        val importedValue = importer.importValue(relativePath, isRelative)
        val typeName = ctx.IDENTIFIER().text
        val name = ctx.importAlias()?.IDENTIFIER()?.text ?: ctx.importPath().relativeImportPath().IDENTIFIER().last().text

        val variableType = types.getType(typeName)

        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, value = importedValue, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(
            Pm2InstructionType.DeclareVariable, variableType = variableType, name = name, lineNumber = ctx.start.line
        ))
    }

    override fun exitImportModel(ctx: ProcModel2Parser.ImportModelContext?) {
        val isRelative = ctx!!.importPath().relativeImportPrefix() != null
        val relativePath = "/" + ctx.importPath().relativeImportPath().text
        val name = ctx.importAlias()?.IDENTIFIER()?.text ?: ctx.importPath().relativeImportPath().IDENTIFIER().last().text
        if (importedModelIDs.containsKey(name)) throw Pm2CompileError("Duplicate import $name")

        importedModelIDs[name] = importer.importModel(relativePath, isRelative)
    }

    override fun exitImportTriangles(ctx: ProcModel2Parser.ImportTrianglesContext?) {
        val isRelative = ctx!!.importPath().relativeImportPrefix() != null
        val relativePath = "/" + ctx.importPath().relativeImportPath().text
        val importedTriangles = importer.importTriangles(relativePath, isRelative)

        val importedValue = when (ctx.IDENTIFIER().text) {
            "triangles" -> importedTriangles.second
            "vertices" -> importedTriangles.first
            else -> throw Pm2CompileError("Expected to import 'triangles' or 'vertices', but got ${ctx.IDENTIFIER().text}")
        }

        val name = ctx.importAlias()?.IDENTIFIER()?.text ?: ctx.importPath().relativeImportPath().IDENTIFIER().last().text

        // TODO Use a Set instead of a List for the vertices, when Set support is added
        instructions.add(Pm2Instruction(
            Pm2InstructionType.PushValue,
            value = Pm2ListValue(importedValue.toMutableList()),
            lineNumber = ctx.start.line
        ))
        instructions.add(Pm2Instruction(
            Pm2InstructionType.DeclareVariable,
            variableType = BuiltinTypes.LIST,
            name = name,
            lineNumber = ctx.stop.line
        ))
    }

    override fun exitChildModel(ctx: ProcModel2Parser.ChildModelContext?) {
        val modelName = ctx!!.IDENTIFIER().text
        val modelID = importedModelIDs[modelName] ?: throw Pm2CompileError("Unknown child model $modelName")
        instructions.add(Pm2Instruction(Pm2InstructionType.CreateChildModel, name = modelID, lineNumber = ctx.start.line))
    }

    override fun exitInnerStatement(ctx: ProcModel2Parser.InnerStatementContext?) {

        // In statements like `functionCall(x);`, the result is ignored, and should therefore be deleted from the stack
        if (ctx!!.functionInvocation() != null) {
            instructions.add(Pm2Instruction(Pm2InstructionType.Delete, lineNumber = ctx.start.line))
        }
    }

    override fun exitVariableDeclaration(ctx: ProcModel2Parser.VariableDeclarationContext?) {
        val typeName = ctx!!.IDENTIFIER(0).text
        val type = types.getType(typeName)
        // TODO Allow types that are not defined yet
        if (ctx.expression() == null) {
            val createDefaultValue = type.createDefaultValue
                    ?: throw Pm2CompileError(
                        "Type $typeName doesn't have a default value", ctx.start.line, ctx.start.charPositionInLine
                    )
            instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = createDefaultValue()))
        }
        instructions.add(Pm2Instruction(
            Pm2InstructionType.DeclareVariable,
            lineNumber = ctx.start.line,
            variableType = type,
            name = ctx.IDENTIFIER(1).text)
        )
    }

    override fun exitVariableReassignmentTarget(ctx: ProcModel2Parser.VariableReassignmentTargetContext?) {
        val identifiers = ctx!!.IDENTIFIER().toMutableList()
        if (identifiers.size > 1) {
            instructions.add(Pm2Instruction(
                Pm2InstructionType.PushVariable,
                lineNumber = ctx.start.line,
                name = identifiers.removeFirst().text)
            )
            while (identifiers.size > 1) {
                instructions.add(Pm2Instruction(
                    Pm2InstructionType.PushProperty,
                    lineNumber = ctx.start.line,
                    name = identifiers.removeFirst().text)
                )
            }
        }
    }

    override fun exitUpdateArrayOrMap(ctx: ProcModel2Parser.UpdateArrayOrMapContext?) {
        instructions.add(Pm2Instruction(Pm2InstructionType.UpdateListOrMap, lineNumber = ctx!!.start.line))
    }

    override fun exitVariableReassignment(ctx: ProcModel2Parser.VariableReassignmentContext?) {
        val identifiers = ctx!!.variableReassignmentTarget().IDENTIFIER().toMutableList()
        val instructionType = if (identifiers.size == 1) Pm2InstructionType.ReassignVariable else Pm2InstructionType.SetProperty
        instructions.add(Pm2Instruction(instructionType, lineNumber = ctx.start.line, name = identifiers.last().text))
    }

    override fun enterListDeclaration(ctx: ProcModel2Parser.ListDeclarationContext?) {
        instructions.add(Pm2Instruction(
            Pm2InstructionType.PushValue, value = Pm2ListValue(mutableListOf()), lineNumber = ctx!!.start.line
        ))
    }

    override fun exitListElement(ctx: ProcModel2Parser.ListElementContext?) {
        instructions.add(Pm2Instruction(
            Pm2InstructionType.InvokeBuiltinFunction, name = "add", lineNumber = ctx!!.start.line
        ))
    }

    override fun exitExpression(ctx: ProcModel2Parser.ExpressionContext?) {
        val nextInstruction = ExpressionComputer.compute(ctx!!)
        if (nextInstruction != null) instructions.add(nextInstruction)
    }

    override fun exitForLoopHeader(ctx: ProcModel2Parser.ForLoopHeaderContext?) {
        loopIndexStack.add(instructions.size)

        instructions.add(Pm2Instruction(Pm2InstructionType.Duplicate, lineNumber = ctx!!.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PushScope, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PushVariable, lineNumber = ctx.start.line, name = ctx.forLoopVariable().text))

        if (ctx.forLoopComparator2().text == "<=") {
            instructions.add(Pm2Instruction(Pm2InstructionType.SmallerThan, lineNumber = ctx.start.line))
        } else {
            instructions.add(Pm2Instruction(Pm2InstructionType.SmallerOrEqual, lineNumber = ctx.start.line))
        }
        // Note: the jumpOffset will be fixed later
        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2IntValue(-1)))
        instructions.add(Pm2Instruction(Pm2InstructionType.Jump, lineNumber = ctx.start.line))
    }

    override fun exitForLoopComparator1(ctx: ProcModel2Parser.ForLoopComparator1Context?) {
        if (ctx!!.text == "<") {
            instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2IntValue(1)))
            instructions.add(Pm2Instruction(Pm2InstructionType.Add, lineNumber = ctx.start.line))
        } else if (ctx.text != "<=") {
            throw Pm2CompileError("Unexpected lower bound comparator of for loop: ${ctx.text}")
        }
    }

    override fun exitForLoopVariable(ctx: ProcModel2Parser.ForLoopVariableContext?) {
        instructions.add(Pm2Instruction(Pm2InstructionType.PushScope, lineNumber = ctx!!.start.line))
        instructions.add(Pm2Instruction(
            Pm2InstructionType.DeclareVariable,
            lineNumber = ctx.start.line,
            variableType = BuiltinTypes.INT,
            name = ctx.text
        ))
    }

    override fun exitForLoop(ctx: ProcModel2Parser.ForLoopContext?) {
        instructions.add(Pm2Instruction(Pm2InstructionType.PopScope, lineNumber = ctx!!.start.line))
        instructions.add(Pm2Instruction(
            Pm2InstructionType.PushVariable,
            lineNumber = ctx.start.line,
            name = ctx.forLoopHeader().forLoopVariable().text)
        )
        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2IntValue(1)))
        instructions.add(Pm2Instruction(Pm2InstructionType.Add, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(
            Pm2InstructionType.ReassignVariable,
            lineNumber = ctx.start.line,
            name = ctx.forLoopHeader().forLoopVariable().text
        ))

        instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2BooleanValue(true)))
        val jumpBackInstructionIndex = instructions.size
        val targetInstructionIndex = loopIndexStack.removeLast()
        instructions.add(Pm2Instruction(
            Pm2InstructionType.PushValue,
            lineNumber = ctx.start.line,
            value = Pm2IntValue(targetInstructionIndex - jumpBackInstructionIndex - 1)
        ))
        instructions.add(Pm2Instruction(Pm2InstructionType.Jump, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.Delete, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.Delete, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PopScope, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PopScope, lineNumber = ctx.start.line))

        val exitOffsetInstructionIndex = targetInstructionIndex + 4
        instructions[exitOffsetInstructionIndex] = Pm2Instruction(
            Pm2InstructionType.PushValue,
            lineNumber = ctx.start.line,
            value = Pm2IntValue(2 + jumpBackInstructionIndex - exitOffsetInstructionIndex)
        )
    }
}
