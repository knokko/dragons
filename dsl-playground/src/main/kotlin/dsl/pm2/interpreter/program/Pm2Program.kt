package dsl.pm2.interpreter.program

import dsl.pm2.ProcModel2Lexer
import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.instruction.Pm2Instruction
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker
import kotlin.jvm.Throws

class Pm2Program(
    val instructions: List<Pm2Instruction>
) {

    @Throws(Pm2RuntimeError::class)
    fun run(): List<Pm2Vertex> {
        return Pm2Processor().execute(this)
    }

    companion object {
        fun compile(sourceCode: String): Pm2Program {
            val converter = Pm2Converter()

            val lexer = ProcModel2Lexer(CharStreams.fromString(sourceCode))
            val parser = ProcModel2Parser(CommonTokenStream(lexer))
            parser.errorHandler = BailErrorStrategy()

            try {
                val context = parser.start()
                ParseTreeWalker.DEFAULT.walk(converter, context)
            } catch (cancelled: ParseCancellationException) {
                val cause = cancelled.cause
                if (cause is RecognitionException) {
                    throw Pm2CompileError(
                        "unexpected '${cause.offendingToken.text}' at '${cause.ctx.text}'",
                        cause.offendingToken.line, cause.offendingToken.charPositionInLine
                    )
                } else {
                    throw Pm2CompileError(cancelled.message ?: cancelled.cause?.message ?: "unknown")
                }
            }

            return converter.program
        }
    }
}
