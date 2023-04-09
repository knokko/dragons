// Generated from java-escape by ANTLR 4.11.1
package dsl.pm2;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ProcModel2Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ProcModel2Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart(ProcModel2Parser.StartContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#outerStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOuterStatement(ProcModel2Parser.OuterStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#innerStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInnerStatement(ProcModel2Parser.InnerStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#parameterDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterDeclaration(ProcModel2Parser.ParameterDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(ProcModel2Parser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#variableReassignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableReassignment(ProcModel2Parser.VariableReassignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#functionInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionInvocation(ProcModel2Parser.FunctionInvocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ProcModel2Parser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#variableProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableProperty(ProcModel2Parser.VariablePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#positionConstructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionConstructor(ProcModel2Parser.PositionConstructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoop(ProcModel2Parser.ForLoopContext ctx);
}