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
	 * Visit a parse tree produced by {@link ProcModel2Parser#vertexDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVertexDeclaration(ProcModel2Parser.VertexDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#floatDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatDeclaration(ProcModel2Parser.FloatDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#intDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntDeclaration(ProcModel2Parser.IntDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#floatExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatExpression(ProcModel2Parser.FloatExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#intExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntExpression(ProcModel2Parser.IntExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoop(ProcModel2Parser.ForLoopContext ctx);
}