// Generated from java-escape by ANTLR 4.11.1
package dsl.pm2;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ProcModel2Parser}.
 */
public interface ProcModel2Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(ProcModel2Parser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(ProcModel2Parser.StartContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#outerStatement}.
	 * @param ctx the parse tree
	 */
	void enterOuterStatement(ProcModel2Parser.OuterStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#outerStatement}.
	 * @param ctx the parse tree
	 */
	void exitOuterStatement(ProcModel2Parser.OuterStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#innerStatement}.
	 * @param ctx the parse tree
	 */
	void enterInnerStatement(ProcModel2Parser.InnerStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#innerStatement}.
	 * @param ctx the parse tree
	 */
	void exitInnerStatement(ProcModel2Parser.InnerStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#parameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterParameterDeclaration(ProcModel2Parser.ParameterDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#parameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitParameterDeclaration(ProcModel2Parser.ParameterDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#vertexDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVertexDeclaration(ProcModel2Parser.VertexDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#vertexDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVertexDeclaration(ProcModel2Parser.VertexDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#floatDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFloatDeclaration(ProcModel2Parser.FloatDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#floatDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFloatDeclaration(ProcModel2Parser.FloatDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#intDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterIntDeclaration(ProcModel2Parser.IntDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#intDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitIntDeclaration(ProcModel2Parser.IntDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#floatExpression}.
	 * @param ctx the parse tree
	 */
	void enterFloatExpression(ProcModel2Parser.FloatExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#floatExpression}.
	 * @param ctx the parse tree
	 */
	void exitFloatExpression(ProcModel2Parser.FloatExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#intExpression}.
	 * @param ctx the parse tree
	 */
	void enterIntExpression(ProcModel2Parser.IntExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#intExpression}.
	 * @param ctx the parse tree
	 */
	void exitIntExpression(ProcModel2Parser.IntExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#forLoop}.
	 * @param ctx the parse tree
	 */
	void enterForLoop(ProcModel2Parser.ForLoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#forLoop}.
	 * @param ctx the parse tree
	 */
	void exitForLoop(ProcModel2Parser.ForLoopContext ctx);
}