package dsl.pm2.interpreter

import dsl.pm2.ProcModel2Parser
import java.lang.Float.parseFloat

class Pm2FloatExpressionEvaluator {

    private var context = Context(parent = null)

    fun push(ctx: ProcModel2Parser.FloatExpressionContext) {
        this.context = Context(parent = this.context)
    }

    private fun getResult(ctx: ProcModel2Parser.FloatExpressionContext, index: Int) = this.get(ctx.floatExpression(index))

    private fun computeResult(ctx: ProcModel2Parser.FloatExpressionContext): Float {
        if (ctx.FLOAT_LITERAL() != null) return parseFloat(ctx.FLOAT_LITERAL().text)
        if (ctx.IDENTIFIER() != null) TODO("Get variable ${ctx.IDENTIFIER().text}")
        if (ctx.TIMES() != null) return this.getResult(ctx, 0) * this.getResult(ctx, 1)
        if (ctx.PLUS() != null) return this.getResult(ctx, 0) + this.getResult(ctx, 1)
        if (ctx.MINUS() != null) return this.getResult(ctx, 0) - this.getResult(ctx, 1)
        if (ctx.floatExpression().size == 1) return this.context.evaluations[ctx.floatExpression(0)]!!
        throw UnsupportedOperationException("Unknown float expression")
    }

    fun pop(ctx: ProcModel2Parser.FloatExpressionContext) {
        val result = computeResult(ctx)
        this.context = this.context.parent!!
        this.context.evaluations[ctx] = result
    }

    fun get(ctx: ProcModel2Parser.FloatExpressionContext) = this.context.evaluations[ctx]!!
}

private class Context(
    val parent: Context?,
) {
    val evaluations = mutableMapOf<ProcModel2Parser.FloatExpressionContext, Float>()
}
