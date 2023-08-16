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
	 * Enter a parse tree produced by {@link ProcModel2Parser#relativeImportPrefix}.
	 * @param ctx the parse tree
	 */
	void enterRelativeImportPrefix(ProcModel2Parser.RelativeImportPrefixContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#relativeImportPrefix}.
	 * @param ctx the parse tree
	 */
	void exitRelativeImportPrefix(ProcModel2Parser.RelativeImportPrefixContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#relativeImportPath}.
	 * @param ctx the parse tree
	 */
	void enterRelativeImportPath(ProcModel2Parser.RelativeImportPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#relativeImportPath}.
	 * @param ctx the parse tree
	 */
	void exitRelativeImportPath(ProcModel2Parser.RelativeImportPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#importPath}.
	 * @param ctx the parse tree
	 */
	void enterImportPath(ProcModel2Parser.ImportPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#importPath}.
	 * @param ctx the parse tree
	 */
	void exitImportPath(ProcModel2Parser.ImportPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#importModel}.
	 * @param ctx the parse tree
	 */
	void enterImportModel(ProcModel2Parser.ImportModelContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#importModel}.
	 * @param ctx the parse tree
	 */
	void exitImportModel(ProcModel2Parser.ImportModelContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#importTriangles}.
	 * @param ctx the parse tree
	 */
	void enterImportTriangles(ProcModel2Parser.ImportTrianglesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#importTriangles}.
	 * @param ctx the parse tree
	 */
	void exitImportTriangles(ProcModel2Parser.ImportTrianglesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#importAlias}.
	 * @param ctx the parse tree
	 */
	void enterImportAlias(ProcModel2Parser.ImportAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#importAlias}.
	 * @param ctx the parse tree
	 */
	void exitImportAlias(ProcModel2Parser.ImportAliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#importValue}.
	 * @param ctx the parse tree
	 */
	void enterImportValue(ProcModel2Parser.ImportValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#importValue}.
	 * @param ctx the parse tree
	 */
	void exitImportValue(ProcModel2Parser.ImportValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#childModel}.
	 * @param ctx the parse tree
	 */
	void enterChildModel(ProcModel2Parser.ChildModelContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#childModel}.
	 * @param ctx the parse tree
	 */
	void exitChildModel(ProcModel2Parser.ChildModelContext ctx);
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
	 * Enter a parse tree produced by {@link ProcModel2Parser#dynamicDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterDynamicDeclaration(ProcModel2Parser.DynamicDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#dynamicDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitDynamicDeclaration(ProcModel2Parser.DynamicDeclarationContext ctx);
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
	 * Enter a parse tree produced by {@link ProcModel2Parser#variableReassignmentTarget}.
	 * @param ctx the parse tree
	 */
	void enterVariableReassignmentTarget(ProcModel2Parser.VariableReassignmentTargetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#variableReassignmentTarget}.
	 * @param ctx the parse tree
	 */
	void exitVariableReassignmentTarget(ProcModel2Parser.VariableReassignmentTargetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(ProcModel2Parser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(ProcModel2Parser.FunctionDeclarationContext ctx);
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
	 * Enter a parse tree produced by {@link ProcModel2Parser#readArrayOrMap}.
	 * @param ctx the parse tree
	 */
	void enterReadArrayOrMap(ProcModel2Parser.ReadArrayOrMapContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#readArrayOrMap}.
	 * @param ctx the parse tree
	 */
	void exitReadArrayOrMap(ProcModel2Parser.ReadArrayOrMapContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#updateArrayOrMap}.
	 * @param ctx the parse tree
	 */
	void enterUpdateArrayOrMap(ProcModel2Parser.UpdateArrayOrMapContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#updateArrayOrMap}.
	 * @param ctx the parse tree
	 */
	void exitUpdateArrayOrMap(ProcModel2Parser.UpdateArrayOrMapContext ctx);
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
	 * Enter a parse tree produced by {@link ProcModel2Parser#listElement}.
	 * @param ctx the parse tree
	 */
	void enterListElement(ProcModel2Parser.ListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#listElement}.
	 * @param ctx the parse tree
	 */
	void exitListElement(ProcModel2Parser.ListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#listDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterListDeclaration(ProcModel2Parser.ListDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#listDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitListDeclaration(ProcModel2Parser.ListDeclarationContext ctx);
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
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#forLoopHeader}.
	 * @param ctx the parse tree
	 */
	void enterForLoopHeader(ProcModel2Parser.ForLoopHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#forLoopHeader}.
	 * @param ctx the parse tree
	 */
	void exitForLoopHeader(ProcModel2Parser.ForLoopHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#forLoopVariable}.
	 * @param ctx the parse tree
	 */
	void enterForLoopVariable(ProcModel2Parser.ForLoopVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#forLoopVariable}.
	 * @param ctx the parse tree
	 */
	void exitForLoopVariable(ProcModel2Parser.ForLoopVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#forLoopComparator1}.
	 * @param ctx the parse tree
	 */
	void enterForLoopComparator1(ProcModel2Parser.ForLoopComparator1Context ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#forLoopComparator1}.
	 * @param ctx the parse tree
	 */
	void exitForLoopComparator1(ProcModel2Parser.ForLoopComparator1Context ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#forLoopComparator2}.
	 * @param ctx the parse tree
	 */
	void enterForLoopComparator2(ProcModel2Parser.ForLoopComparator2Context ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#forLoopComparator2}.
	 * @param ctx the parse tree
	 */
	void exitForLoopComparator2(ProcModel2Parser.ForLoopComparator2Context ctx);
	/**
	 * Enter a parse tree produced by {@link ProcModel2Parser#forLoopComparator}.
	 * @param ctx the parse tree
	 */
	void enterForLoopComparator(ProcModel2Parser.ForLoopComparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProcModel2Parser#forLoopComparator}.
	 * @param ctx the parse tree
	 */
	void exitForLoopComparator(ProcModel2Parser.ForLoopComparatorContext ctx);
}