package dsl.pm2.interpreter

import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.Pm2FloatValue
import dsl.pm2.interpreter.value.Pm2IntValue
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt

object ExpressionComputer {
    fun compute(ctx: ProcModel2Parser.ExpressionContext): Pm2Instruction? {
        if (ctx.FLOAT_LITERAL() != null) return Pm2Instruction(
            Pm2InstructionType.PushValue, value = Pm2FloatValue(parseFloat(ctx.FLOAT_LITERAL().text))
        )
        if (ctx.INT_LITERAL() != null) return Pm2Instruction(
            Pm2InstructionType.PushValue, value = Pm2IntValue(parseInt(ctx.INT_LITERAL().text))
        )
        if (ctx.variableProperty() != null) return Pm2Instruction(
            Pm2InstructionType.PushProperty, name = ctx.variableProperty().IDENTIFIER().text
        )

        if (ctx.DIVIDE() != null) return Pm2Instruction(Pm2InstructionType.Divide)
        if (ctx.TIMES() != null) return Pm2Instruction(Pm2InstructionType.Multiply)
        if (ctx.PLUS() != null) return Pm2Instruction(Pm2InstructionType.Add)
        if (ctx.MINUS() != null) return Pm2Instruction(Pm2InstructionType.Subtract)

        if (ctx.IDENTIFIER() != null) return Pm2Instruction(
            Pm2InstructionType.PushVariable, name = ctx.IDENTIFIER().text
        )

        // The position constructor is just syntactic sugar
        if (ctx.positionConstructor() != null) return Pm2Instruction(
            Pm2InstructionType.InvokeBuiltinFunction, name = "constructPosition"
        )

        return null
    }
}