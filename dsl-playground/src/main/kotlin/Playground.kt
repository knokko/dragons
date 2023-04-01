import dsl.test.Test1BaseListener
import dsl.test.Test1Lexer
import dsl.test.Test1Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

fun main() {
    val lexer = Test1Lexer(CharStreams.fromString("1+2*5"))
    val parser = Test1Parser(CommonTokenStream(lexer))
    val context = parser.start()

    val listener = PlaygroundListener()
    ParseTreeWalker.DEFAULT.walk(listener, context)
}

class PlaygroundListener : Test1BaseListener() {

    override fun enterExpression(ctx: Test1Parser.ExpressionContext?) {
        println("enterExpression(${ctx!!.text})")
        println(ctx.INT())
    }

    override fun exitExpression(ctx: Test1Parser.ExpressionContext?) {
        println("exitExpression(${ctx!!.text})")
    }
}

