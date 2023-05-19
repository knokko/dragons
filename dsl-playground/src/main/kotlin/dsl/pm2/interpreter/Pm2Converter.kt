package dsl.pm2.interpreter

import dsl.pm2.ProcModel2BaseListener
import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.interpreter.value.Pm2BooleanValue
import dsl.pm2.interpreter.value.Pm2IntValue
import org.antlr.v4.runtime.tree.ErrorNode

class Pm2Converter : ProcModel2BaseListener() {

    private val instructions = mutableListOf<Pm2Instruction>()
    private val types = Pm2Types()
    private val functions = Pm2Functions()

    private val loopIndexStack = mutableListOf<Int>()

    lateinit var program: Pm2Program

    override fun visitErrorNode(node: ErrorNode?) {
        println("Encountered error $node")
    }

    override fun enterStart(ctx: ProcModel2Parser.StartContext?) {
        types.pushScope()
        types.defineType("float", BuiltinTypes.FLOAT)
        types.defineType("int", BuiltinTypes.INT)
        types.defineType("position", BuiltinTypes.POSITION)
        types.defineType("Vertex", BuiltinTypes.VERTEX)

        functions.pushScope()
    }

    override fun exitStart(ctx: ProcModel2Parser.StartContext?) {
        types.popScope()
        functions.popScope()
        program = Pm2Program(instructions)
    }

    override fun enterParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun exitParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        TODO("Not yet implemented")
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
            println(functionName)
            TODO("Add support for user-defined functions")
        }
//        val function = context.getFunction(functionName) ?: throw IllegalArgumentException("Unknown function $functionName")
//
//        val parameters = Array(ctx.expression().size) { this.expressionEvaluator.pop() }.reversed()
//        function.invokeChecked(parameters)
    }

    override fun exitInnerStatement(ctx: ProcModel2Parser.InnerStatementContext?) {

        // In statements like `functionCall(x);`, the result is ignored, and should therefore be deleted from the stack
        if (ctx!!.functionInvocation() != null) {
            instructions.add(Pm2Instruction(Pm2InstructionType.Delete, lineNumber = ctx.start.line))
        }
    }

    override fun exitVariableDeclaration(ctx: ProcModel2Parser.VariableDeclarationContext?) {
        val typeName = ctx!!.IDENTIFIER(0).text
        val type = types.getType(typeName) ?: throw Pm2CompileError(
            "Unknown type $typeName", ctx.start.line, ctx.start.charPositionInLine
        ) // TODO Allow types that are not defined yet
        if (ctx.expression() == null) {
            if (type.createDefaultValue == null) throw Pm2CompileError(
                "Type $typeName doesn't have a default value", ctx.start.line, ctx.start.charPositionInLine
            )
            instructions.add(Pm2Instruction(Pm2InstructionType.PushValue, lineNumber = ctx.start.line, variableType = type))
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

    override fun exitVariableReassignment(ctx: ProcModel2Parser.VariableReassignmentContext?) {
        val identifiers = ctx!!.variableReassignmentTarget().IDENTIFIER().toMutableList()
        val instructionType = if (identifiers.size == 1) Pm2InstructionType.ReassignVariable else Pm2InstructionType.SetProperty
        instructions.add(Pm2Instruction(instructionType, lineNumber = ctx.start.line, name = identifiers.last().text))
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
        instructions.add(Pm2Instruction(Pm2InstructionType.Jump, lineNumber = ctx.start.line, jumpOffset = -1))
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
            Pm2InstructionType.Jump,
            lineNumber = ctx.start.line,
            jumpOffset = targetInstructionIndex - jumpBackInstructionIndex
        ))
        instructions.add(Pm2Instruction(Pm2InstructionType.Delete, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PopScope, lineNumber = ctx.start.line))
        instructions.add(Pm2Instruction(Pm2InstructionType.PopScope, lineNumber = ctx.start.line))

        val exitInstructionIndex = targetInstructionIndex + 4
        instructions[exitInstructionIndex] = Pm2Instruction(
            Pm2InstructionType.Jump,
            lineNumber = ctx.start.line,
            jumpOffset = 1 + jumpBackInstructionIndex - exitInstructionIndex
        )
    }

    fun dumpInstructions() {
        for (instruction in instructions) {
            println(instruction)
        }
    }
}