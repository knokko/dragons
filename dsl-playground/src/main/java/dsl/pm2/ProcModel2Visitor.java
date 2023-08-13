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
	 * Visit a parse tree produced by {@link ProcModel2Parser#relativeImportPrefix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelativeImportPrefix(ProcModel2Parser.RelativeImportPrefixContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#relativeImportPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelativeImportPath(ProcModel2Parser.RelativeImportPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#importPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportPath(ProcModel2Parser.ImportPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#importModel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportModel(ProcModel2Parser.ImportModelContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#importAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAlias(ProcModel2Parser.ImportAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#importValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportValue(ProcModel2Parser.ImportValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#childModel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChildModel(ProcModel2Parser.ChildModelContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#parameterDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterDeclaration(ProcModel2Parser.ParameterDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#dynamicDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDynamicDeclaration(ProcModel2Parser.DynamicDeclarationContext ctx);
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
	 * Visit a parse tree produced by {@link ProcModel2Parser#variableReassignmentTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableReassignmentTarget(ProcModel2Parser.VariableReassignmentTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(ProcModel2Parser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#functionInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionInvocation(ProcModel2Parser.FunctionInvocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#readArrayOrMap}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadArrayOrMap(ProcModel2Parser.ReadArrayOrMapContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#updateArrayOrMap}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdateArrayOrMap(ProcModel2Parser.UpdateArrayOrMapContext ctx);
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
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoopHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoopHeader(ProcModel2Parser.ForLoopHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoopVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoopVariable(ProcModel2Parser.ForLoopVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoopComparator1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoopComparator1(ProcModel2Parser.ForLoopComparator1Context ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoopComparator2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoopComparator2(ProcModel2Parser.ForLoopComparator2Context ctx);
	/**
	 * Visit a parse tree produced by {@link ProcModel2Parser#forLoopComparator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoopComparator(ProcModel2Parser.ForLoopComparatorContext ctx);
}