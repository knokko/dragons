package dsl.pm2.interpreter

import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.Pm2FloatValue
import dsl.pm2.interpreter.value.Pm2IntValue
import dsl.pm2.interpreter.value.Pm2StringValue
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt

object ExpressionComputer {
    fun compute(ctx: ProcModel2Parser.ExpressionContext): Pm2Instruction? {
        if (ctx.FLOAT_LITERAL() != null) return Pm2Instruction(
            Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2FloatValue(parseFloat(ctx.FLOAT_LITERAL().text))
        )
        if (ctx.INT_LITERAL() != null) return Pm2Instruction(
            Pm2InstructionType.PushValue, lineNumber = ctx.start.line, value = Pm2IntValue(parseInt(ctx.INT_LITERAL().text))
        )
        if (ctx.STRING_LITERAL() != null) {
            val literal = ctx.STRING_LITERAL().text
            return Pm2Instruction(
                Pm2InstructionType.PushValue, lineNumber = ctx.start.line,
                value = Pm2StringValue(literal.substring(1 until literal.length - 1))
            )
        }
        if (ctx.variableProperty() != null) return Pm2Instruction(
            Pm2InstructionType.PushProperty, lineNumber = ctx.start.line, name = ctx.variableProperty().IDENTIFIER().text
        )
        if (ctx.readArrayOrMap() != null) return Pm2Instruction(Pm2InstructionType.ReadArrayOrMap, lineNumber = ctx.start.line)

        if (ctx.DIVIDE() != null) return Pm2Instruction(Pm2InstructionType.Divide, lineNumber = ctx.start.line)
        if (ctx.TIMES() != null) return Pm2Instruction(Pm2InstructionType.Multiply, lineNumber = ctx.start.line)
        if (ctx.PLUS() != null) return Pm2Instruction(Pm2InstructionType.Add, lineNumber = ctx.start.line)
        if (ctx.MINUS() != null) return Pm2Instruction(Pm2InstructionType.Subtract, lineNumber = ctx.start.line)

        if (ctx.IDENTIFIER() != null) return Pm2Instruction(
            Pm2InstructionType.PushVariable, lineNumber = ctx.start.line, name = ctx.IDENTIFIER().text
        )

        // The position constructor is just syntactic sugar
        if (ctx.positionConstructor() != null) return Pm2Instruction(
            Pm2InstructionType.InvokeBuiltinFunction, lineNumber = ctx.start.line, name = "constructPosition"
        )

        return null
    }
}