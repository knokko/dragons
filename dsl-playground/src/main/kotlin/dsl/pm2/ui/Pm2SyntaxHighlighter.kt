package dsl.pm2.ui

import dsl.pm2.ProcModel2BaseListener
import dsl.pm2.ProcModel2Lexer
import dsl.pm2.ProcModel2Parser
import dsl.pm2.interpreter.BuiltinTypes
import dsl.pm2.interpreter.Pm2BuiltinFunction
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.RectangularDrawnRegion
import gruviks.component.text.TextAreaStyle
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

private class Pm2SyntaxHighlighter(
    sourceCode: List<String>,
    private val colorTheme: Pm2ColorTheme,
): ProcModel2BaseListener(), ANTLRErrorListener {

    private val mutations = mutableMapOf<Int, MutableList<ColorMutation>>()

    init {
        val rawSourceCode = sourceCode.joinToString("\n")

        val lexer = ProcModel2Lexer(CharStreams.fromString(rawSourceCode))
        lexer.removeErrorListeners()
        lexer.addErrorListener(this)

        val parser = ProcModel2Parser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(this)

        val context = parser.start()
        ParseTreeWalker.DEFAULT.walk(this, context)
    }

    private fun insert(color: Color, lineNumber: Int, charIndex: Int, length: Int) {
        val line = mutations.getOrPut(lineNumber) { mutableListOf() }
        line.add(ColorMutation(color, charIndex, charIndex + length - 1))
    }

    private fun insert(color: Color, start: Token, stop: Token) {
        if (start.line == stop.line) {
            insert(color, start.line, start.charPositionInLine, 1 + stop.stopIndex - start.startIndex)
        } else println("Skipped multiline color mutation")
    }

    private fun getColor(lineNumber: Int, charIndex: Int): Color {
        val lineMutations = mutations[lineNumber] ?: return colorTheme.defaultText

        for (mutation in lineMutations) {
            if (mutation.startIndex <= charIndex && mutation.stopIndex >= charIndex) return mutation.color
        }

        return colorTheme.defaultText
    }

    private fun insertChildKeyword(children: List<ParseTree>, childIndex: Int, keyword: String, color: Color) {
        if (childIndex < children.size) {
            val child = children[childIndex]
            if (child is TerminalNode && child.text == keyword) insert(color, child.symbol, child.symbol)
        }
    }

    private fun insertTypeName(node: TerminalNode) {
        val color = if (node.text == "Any") colorTheme.error
        else if (BuiltinTypes.ALL_TYPES.find { it.name == node.text } != null) colorTheme.builtinTypeName
        else colorTheme.customTypeName

        insert(color, node.symbol, node.symbol)
    }

    private fun insertTypeName(children: List<ParseTree>, childIndex: Int) {
        if (childIndex < children.size) {
            val node = children[childIndex]
            if (node is TerminalNode) insertTypeName(node)
        }
    }

    override fun exitImportModel(ctx: ProcModel2Parser.ImportModelContext?) {
        insertChildKeyword(ctx!!.children, 0, "import", colorTheme.keyword)
        insertChildKeyword(ctx.children, 1, "model", colorTheme.keyword)
    }

    override fun exitImportTriangles(ctx: ProcModel2Parser.ImportTrianglesContext?) {
        insertChildKeyword(ctx!!.children, 0, "import", colorTheme.keyword)
        insertChildKeyword(ctx.children, 1, "triangles", colorTheme.keyword)
        insertChildKeyword(ctx.children, 1, "vertices", colorTheme.keyword)
    }

    override fun exitImportAlias(ctx: ProcModel2Parser.ImportAliasContext?) {
        insertChildKeyword(ctx!!.children, 0, "as", colorTheme.keyword)
    }

    override fun exitImportValue(ctx: ProcModel2Parser.ImportValueContext?) {
        insertChildKeyword(ctx!!.children, 0, "import", colorTheme.keyword)
        insertTypeName(ctx.children, 1)
        insertChildKeyword(ctx.children, 2, "value", colorTheme.keyword)
    }

    override fun exitChildModel(ctx: ProcModel2Parser.ChildModelContext?) {
        insertChildKeyword(ctx!!.children, 0, "child", colorTheme.keyword)
        insertChildKeyword(ctx.children, 1, "model", colorTheme.keyword)
    }

    override fun exitParameterDeclaration(ctx: ProcModel2Parser.ParameterDeclarationContext?) {
        insertChildKeyword(ctx!!.children, 0, "static", colorTheme.keyword)
        insertChildKeyword(ctx.children, 0, "dynamic", colorTheme.keyword)
        insertChildKeyword(ctx.children, 1, "parameter", colorTheme.keyword)
        insertTypeName(ctx.children, 2)
    }

    override fun exitDynamicDeclaration(ctx: ProcModel2Parser.DynamicDeclarationContext?) {
        insertChildKeyword(ctx!!.children, 0, "dynamic", colorTheme.keyword)
        insertChildKeyword(ctx.children, 0, "static", colorTheme.error)

        val identifiers = ctx.IDENTIFIER()
        insertTypeName(identifiers[0])
        for (index in 1 until identifiers.size step 2) insertTypeName(identifiers[index])
    }

    override fun exitForLoopHeader(ctx: ProcModel2Parser.ForLoopHeaderContext?) {
        insertChildKeyword(ctx!!.children, 0, "for", colorTheme.keyword)
    }

    override fun exitFunctionDeclaration(ctx: ProcModel2Parser.FunctionDeclarationContext?) {
        val identifiers = ctx!!.IDENTIFIER()
        insert(colorTheme.functionDeclaration, identifiers[1].symbol, identifiers[1].symbol)
        for (index in 0 until identifiers.size step 2) insertTypeName(identifiers[index])
    }

    override fun exitFunctionInvocation(ctx: ProcModel2Parser.FunctionInvocationContext?) {
        val functionName = ctx!!.IDENTIFIER().text
        if (Pm2BuiltinFunction.MAP.containsKey(functionName)) {
            insert(colorTheme.builtinFunctionCall, ctx.IDENTIFIER().symbol, ctx.IDENTIFIER().symbol)
        }
    }

    override fun exitVariableDeclaration(ctx: ProcModel2Parser.VariableDeclarationContext?) {
        insertTypeName(ctx!!.IDENTIFIER(0))
    }

    override fun exitExpression(ctx: ProcModel2Parser.ExpressionContext?) {
        if (ctx!!.FLOAT_LITERAL() != null) insert(colorTheme.floatLiteral, ctx.start, ctx.stop)
        if (ctx.INT_LITERAL() != null) insert(colorTheme.intLiteral, ctx.start, ctx.stop)
        if (ctx.STRING_LITERAL() != null) insert(colorTheme.stringLiteral, ctx.start, ctx.stop)
    }

    fun getTextStyle(lineIndex: Int) = TextStyle(
        fillColor = colorTheme.defaultText,
        fillColorFunction = { charIndex -> getColor(lineIndex + 1, charIndex) },
        font = colorTheme.font
    )

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        if (offendingSymbol is Token) insert(colorTheme.error, offendingSymbol, offendingSymbol)
    }

    override fun reportAmbiguity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        exact: Boolean,
        ambigAlts: BitSet?,
        configs: ATNConfigSet?
    ) {}

    override fun reportAttemptingFullContext(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        conflictingAlts: BitSet?,
        configs: ATNConfigSet?
    ) {}

    override fun reportContextSensitivity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        prediction: Int,
        configs: ATNConfigSet?
    ) {}
}

private class ColorMutation(val color: Color, val startIndex: Int, val stopIndex: Int)

fun pm2SyntaxTextAreaStyle(
    colorTheme: Pm2ColorTheme,
    lineHeight: Float,
) = TextAreaStyle(
    defaultTextStyle = { allLines -> Pm2SyntaxHighlighter(allLines, colorTheme)::getTextStyle },
    focusTextStyle = { allLines -> Pm2SyntaxHighlighter(allLines, colorTheme)::getTextStyle },
    placeholderTextStyle = { { TextStyle(fillColor = colorTheme.defaultText, font = colorTheme.font) }},
    drawDefaultBackground = { target ->
        target.fillRect(0f, 0f, 1f, 1f, colorTheme.background)
        Pair(RectangularDrawnRegion(0f, 0f, 1f, 1f), lineHeight)
    }, drawFocusBackground = { target ->
        target.fillRect(0f, 0f, 1f, 1f, colorTheme.background)
        Pair(RectangularDrawnRegion(0f, 0f, 1f, 1f), lineHeight)
    }
)
