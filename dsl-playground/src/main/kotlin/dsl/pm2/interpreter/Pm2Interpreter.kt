package dsl.pm2.interpreter

import dsl.pm2.ProcModel2BaseListener
import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.value.Pm2VertexValue
import org.antlr.v4.runtime.tree.ErrorNode

class Pm2Interpreter : ProcModel2BaseListener() {

    private val context = Pm2Context()
    private val expressionEvaluator = Pm2ExpressionEvaluator(context)

    val vertices = mutableListOf<Pm2VertexValue>()

    override fun visitErrorNode(node: ErrorNode?) {
        println("Encountered error $node")
    }

    override fun enterStart(ctx: ProcModel2Parser.StartContext?) {
        println("start")
        context.pushScope()
        context.defineType("float", BuiltinTypes.FLOAT)
        context.defineType("int", BuiltinTypes.INT)
        context.defineType("position", BuiltinTypes.POSITION)
        context.defineType("Vertex", BuiltinTypes.VERTEX)
        context.defineFunction("produceTriangle", BuiltinFunctions.produceTriangle { vertex1, vertex2, vertex3 ->
            vertices.add(vertex1)
            vertices.add(vertex2)
            vertices.add(vertex3)
        })
    }

    override fun exitStart(ctx: ProcModel2Parser.StartContext?) {
        println("end")
        context.popScope()
    }

    override fun enterParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun exitParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        TODO("Not yet implemented")
    }

    override fun exitFunctionInvocation(ctx: ProcModel2Parser.FunctionInvocationContext?) {
        val functionName = ctx!!.IDENTIFIER().text
        val function = context.getFunction(functionName) ?: throw IllegalArgumentException("Unknown function $functionName")

        val parameters = ctx.expression().map(expressionEvaluator::get)
        function.invokeChecked(parameters)
        println("called function $functionName with parameters $parameters")
    }

    override fun exitVariableDeclaration(ctx: ProcModel2Parser.VariableDeclarationContext?) {
        if (ctx!!.expression() != null) {
            context.declareVariable(ctx.IDENTIFIER(1).text, ctx.IDENTIFIER(0).text, expressionEvaluator.get(ctx.expression()))
        } else {
            context.declareVariable(ctx.IDENTIFIER(1).text, ctx.IDENTIFIER(0).text)
        }
    }

    override fun exitVariableReassignment(ctx: ProcModel2Parser.VariableReassignmentContext?) {
        val identifiers = ctx!!.IDENTIFIER().toMutableList()
        val newValue = this.expressionEvaluator.get(ctx.expression())
        if (identifiers.size == 1) {
            context.reassignVariable(identifiers[0].text, newValue)
        } else {
            var variable = context.getVariable(identifiers.removeFirst().text)?.second
                ?: throw IllegalArgumentException("Unknown variable")
            while (identifiers.size > 1) {
                variable = variable.getProperty(identifiers.removeFirst().text)
            }
            variable.setProperty(identifiers[0].text, newValue)
        }
    }

    override fun enterExpression(ctx: ProcModel2Parser.ExpressionContext?) {
        expressionEvaluator.push(ctx!!)
    }

    override fun exitExpression(ctx: ProcModel2Parser.ExpressionContext?) {
        expressionEvaluator.pop(ctx!!)
    }

    override fun enterForLoop(ctx: ProcModel2Parser.ForLoopContext?) {
        TODO("Not yet implemented")
    }

    override fun exitForLoop(ctx: ProcModel2Parser.ForLoopContext?) {
        TODO("Not yet implemented")
    }
}
