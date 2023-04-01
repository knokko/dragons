package dsl.pm2.interpreter

import dsl.pm2.ProcModel2Lexer
import dsl.pm2.ProcModel2Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

fun main() {
    val lexer = ProcModel2Lexer(CharStreams.fromString("float f = 1.2 - 3.3 * 3.0 + (1.5 - 0.5) * 10.0;"))
    val parser = ProcModel2Parser(CommonTokenStream(lexer))
    val context = parser.start()

    val listener = Pm2Interpreter()
    ParseTreeWalker.DEFAULT.walk(listener, context)
}
