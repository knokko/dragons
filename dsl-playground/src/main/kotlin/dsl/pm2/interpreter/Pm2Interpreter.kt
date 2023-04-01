package dsl.pm2.interpreter

import dsl.pm2.ProcModel2BaseListener
import dsl.pm2.ProcModel2Parser
import org.antlr.v4.runtime.tree.ErrorNode

class Pm2Interpreter : ProcModel2BaseListener() {

    private val floatEvaluator = Pm2FloatExpressionEvaluator()

    override fun visitErrorNode(node: ErrorNode?) {
        println("Encountered error $node")
    }

    override fun enterStart(ctx: ProcModel2Parser.StartContext?) {
        println("start")
    }

    override fun exitStart(ctx: ProcModel2Parser.StartContext?) {
        println("end")
    }

    override fun enterParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun exitParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun enterVertexDeclaration(ctx: ProcModel2Parser.VertexDeclarationContext?) {
        println("vertex(${ctx!!.floatExpression(0)})")
    }

    override fun exitVertexDeclaration(ctx: ProcModel2Parser.VertexDeclarationContext?) {
        println("endVertex")
    }

    override fun enterFloatDeclaration(ctx: ProcModel2Parser.FloatDeclarationContext?) {
        println("declare float...")
    }

    override fun exitFloatDeclaration(ctx: ProcModel2Parser.FloatDeclarationContext?) {
        println("declared float ${this.floatEvaluator.get(ctx!!.floatExpression())}")
    }

    override fun enterIntDeclaration(ctx: ProcModel2Parser.IntDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun exitIntDeclaration(ctx: ProcModel2Parser.IntDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun enterFloatExpression(ctx: ProcModel2Parser.FloatExpressionContext?) {
        floatEvaluator.push(ctx!!)
    }

    override fun exitFloatExpression(ctx: ProcModel2Parser.FloatExpressionContext?) {
        floatEvaluator.pop(ctx!!)
    }

    override fun enterIntExpression(ctx: ProcModel2Parser.IntExpressionContext?) {
        println("enterIntExpression")
    }

    override fun exitIntExpression(ctx: ProcModel2Parser.IntExpressionContext?) {
        println("exitIntExpression")
    }

    override fun enterForLoop(ctx: ProcModel2Parser.ForLoopContext?) {
        TODO("Not yet implemented")
    }

    override fun exitForLoop(ctx: ProcModel2Parser.ForLoopContext?) {
        TODO("Not yet implemented")
    }
}
