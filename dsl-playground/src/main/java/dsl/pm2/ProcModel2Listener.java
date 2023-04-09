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
	 * Enter a parse tree produced by {@link ProcModel2Parser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaration(ProcModel2Parser.VariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaration(ProcModel2Parser.VariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#variableReassignment}.
	 * @param ctx the parse tree
	 */
	void enterVariableReassignment(ProcModel2Parser.VariableReassignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#variableReassignment}.
	 * @param ctx the parse tree
	 */
	void exitVariableReassignment(ProcModel2Parser.VariableReassignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#functionInvocation}.
	 * @param ctx the parse tree
	 */
	void enterFunctionInvocation(ProcModel2Parser.FunctionInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#functionInvocation}.
	 * @param ctx the parse tree
	 */
	void exitFunctionInvocation(ProcModel2Parser.FunctionInvocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ProcModel2Parser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ProcModel2Parser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#variableProperty}.
	 * @param ctx the parse tree
	 */
	void enterVariableProperty(ProcModel2Parser.VariablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#variableProperty}.
	 * @param ctx the parse tree
	 */
	void exitVariableProperty(ProcModel2Parser.VariablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#positionConstructor}.
	 * @param ctx the parse tree
	 */
	void enterPositionConstructor(ProcModel2Parser.PositionConstructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#positionConstructor}.
	 * @param ctx the parse tree
	 */
	void exitPositionConstructor(ProcModel2Parser.PositionConstructorContext ctx);
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