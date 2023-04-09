package dsl.pm2.interpreter

import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.value.Pm2FloatValue
import dsl.pm2.interpreter.value.Pm2IntValue
import dsl.pm2.interpreter.value.Pm2PositionValue
import dsl.pm2.interpreter.value.Pm2Value
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt

class Pm2ExpressionEvaluator(
    private val context: Pm2Context
) {

    private var stackTop = StackEntry(parent = null)

    fun push(ctx: ProcModel2Parser.ExpressionContext) {
        this.stackTop = StackEntry(parent = this.stackTop)
    }

    private fun getResult(ctx: ProcModel2Parser.ExpressionContext, index: Int) = this.get(ctx.expression(index))

    private fun computeResult(ctx: ProcModel2Parser.ExpressionContext): Pm2Value {
        if (ctx.FLOAT_LITERAL() != null) return Pm2FloatValue(parseFloat(ctx.FLOAT_LITERAL().text))
        if (ctx.INT_LITERAL() != null) return Pm2IntValue(parseInt(ctx.INT_LITERAL().text))
        if (ctx.variableProperty() != null) return this.getResult(ctx, 0).getProperty(ctx.variableProperty().IDENTIFIER().text)
        if (ctx.DIVIDE() != null) return this.getResult(ctx, 0) / this.getResult(ctx, 0)
        if (ctx.TIMES() != null) return this.getResult(ctx, 0) * this.getResult(ctx, 1)
        if (ctx.PLUS() != null) return this.getResult(ctx, 0) + this.getResult(ctx, 1)
        if (ctx.MINUS() != null) return this.getResult(ctx, 0) - this.getResult(ctx, 1)
        if (ctx.IDENTIFIER() != null) return this.context.getVariable(ctx.IDENTIFIER().text)?.second
            ?: throw UnsupportedOperationException("Unknown variable ${ctx.IDENTIFIER().text}")
        if (ctx.positionConstructor() != null) return Pm2PositionValue(
            this.get(ctx.positionConstructor().expression(0)).floatValue(),
            this.get(ctx.positionConstructor().expression(1)).floatValue()
        )
        if (ctx.expression().size == 1) return this.getResult(ctx, 0)
        throw UnsupportedOperationException("Unknown float expression")
    }

    fun pop(ctx: ProcModel2Parser.ExpressionContext) {
        val result = computeResult(ctx)
        this.stackTop = this.stackTop.parent!!
        this.stackTop.evaluations[ctx] = result
    }

    fun get(ctx: ProcModel2Parser.ExpressionContext) = this.stackTop.evaluations[ctx]!!
}

private class StackEntry(
    val parent: StackEntry?,
) {
    val evaluations = mutableMapOf<ProcModel2Parser.ExpressionContext, Pm2Value>()
}
