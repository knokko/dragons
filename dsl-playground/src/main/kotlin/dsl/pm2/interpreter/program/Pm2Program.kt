package dsl.pm2.interpreter.program

import dsl.pm2.ProcModel2Lexer
import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.*
import dsl.pm2.interpreter.instruction.Pm2Instruction
import dsl.pm2.interpreter.instruction.Pm2InstructionType
import dsl.pm2.interpreter.value.Pm2Value
import dsl.pm2.interpreter.value.Pm2VertexValue
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.InputMismatchException
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker

class Pm2Program(
    val instructions: List<Pm2Instruction>
) {

    fun run(): Pm2Scene {
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
                converter.dumpInstructions()
            } catch (cancelled: ParseCancellationException) {
                val cause = cancelled.cause
                if (cause is InputMismatchException) {
                    throw Pm2CompileError(
                        "mismatch: unexpected '${cause.offendingToken.text}' at '${cause.ctx.text}'",
                        cause.offendingToken.line, cause.offendingToken.charPositionInLine
                    )
                } else {
                    throw Pm2CompileError(cancelled.message!!)
                }
            }

            return converter.program
        }
    }
}
