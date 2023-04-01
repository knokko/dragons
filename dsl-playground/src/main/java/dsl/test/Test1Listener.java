// Generated from java-escape by ANTLR 4.11.1
package dsl.test;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Test1Parser}.
 */
public interface Test1Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Test1Parser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(Test1Parser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link Test1Parser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(Test1Parser.StartContext ctx);
	/**
	 * Enter a parse tree produced by {@link Test1Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(Test1Parser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Test1Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(Test1Parser.ExpressionContext ctx);
}