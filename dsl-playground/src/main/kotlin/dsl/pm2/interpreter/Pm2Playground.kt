package dsl.pm2.interpreter

import dsl.pm2.ProcModel2Lexer
import dsl.pm2.ProcModel2Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

private val program = """
    
    float minX = 2.0 * 0.1;
    float minY;
    float maxY = minY + 1.0;
    
    Vertex bottomLeft;
    bottomLeft.position = (minX, minY);
    
    Vertex bottomRight;
    bottomRight.position = (minX + 0.4, maxY);
    
    maxY = 1.3;
    
    Vertex topRight;
    topRight.position = (minX + 0.5, maxY);
    
    Vertex topLeft;
    topLeft.position = (minX, topRight.position.y);
    
    produceTriangle(bottomLeft, bottomRight, topRight);
    produceTriangle(topRight, topLeft, bottomLeft);
""".trimIndent()

fun main() {
    val lexer = ProcModel2Lexer(CharStreams.fromString(program))
    val parser = ProcModel2Parser(CommonTokenStream(lexer))
    val context = parser.start()

    val listener = Pm2Interpreter()
    ParseTreeWalker.DEFAULT.walk(listener, context)
}
