// Generated from /home/knokko/programming/kotlin/dragons/dsl-playground/src/main/resources/ProcModel2.g4 by ANTLR 4.12.0
package dsl.pm2;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class ProcModel2Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, PLUS=15, MINUS=16, TIMES=17, 
		DIVIDE=18, NORMAL_TYPE=19, PARAMETER_TYPE=20, IDENTIFIER=21, FLOAT_LITERAL=22, 
		INT_LITERAL=23, WS=24;
	public static final int
		RULE_start = 0, RULE_outerStatement = 1, RULE_innerStatement = 2, RULE_parameterDeclaration = 3, 
		RULE_dynamicDeclaration = 4, RULE_variableDeclaration = 5, RULE_variableReassignment = 6, 
		RULE_variableReassignmentTarget = 7, RULE_functionDeclaration = 8, RULE_functionInvocation = 9, 
		RULE_expression = 10, RULE_variableProperty = 11, RULE_positionConstructor = 12, 
		RULE_forLoop = 13, RULE_forLoopHeader = 14, RULE_forLoopVariable = 15, 
		RULE_forLoopComparator1 = 16, RULE_forLoopComparator2 = 17, RULE_forLoopComparator = 18;
	private static String[] makeRuleNames() {
		return new String[] {
			"start", "outerStatement", "innerStatement", "parameterDeclaration", 
			"dynamicDeclaration", "variableDeclaration", "variableReassignment", 
			"variableReassignmentTarget", "functionDeclaration", "functionInvocation", 
			"expression", "variableProperty", "positionConstructor", "forLoop", "forLoopHeader", 
			"forLoopVariable", "forLoopComparator1", "forLoopComparator2", "forLoopComparator"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'parameter'", "'dynamic'", "'<'", "','", "'>'", "'{'", 
			"'}'", "'='", "'.'", "'('", "')'", "'for'", "'<='", "'+'", "'-'", "'*'", 
			"'/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, "PLUS", "MINUS", "TIMES", "DIVIDE", "NORMAL_TYPE", 
			"PARAMETER_TYPE", "IDENTIFIER", "FLOAT_LITERAL", "INT_LITERAL", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "ProcModel2.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ProcModel2Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StartContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(ProcModel2Parser.EOF, 0); }
		public List<OuterStatementContext> outerStatement() {
			return getRuleContexts(OuterStatementContext.class);
		}
		public OuterStatementContext outerStatement(int i) {
			return getRuleContext(OuterStatementContext.class,i);
		}
		public StartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_start; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitStart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitStart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StartContext start() throws RecognitionException {
		StartContext _localctx = new StartContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_start);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3153920L) != 0)) {
				{
				{
				setState(38);
				outerStatement();
				}
				}
				setState(43);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(44);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OuterStatementContext extends ParserRuleContext {
		public ParameterDeclarationContext parameterDeclaration() {
			return getRuleContext(ParameterDeclarationContext.class,0);
		}
		public InnerStatementContext innerStatement() {
			return getRuleContext(InnerStatementContext.class,0);
		}
		public OuterStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_outerStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterOuterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitOuterStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitOuterStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OuterStatementContext outerStatement() throws RecognitionException {
		OuterStatementContext _localctx = new OuterStatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_outerStatement);
		try {
			setState(48);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PARAMETER_TYPE:
				enterOuterAlt(_localctx, 1);
				{
				setState(46);
				parameterDeclaration();
				}
				break;
			case T__12:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(47);
				innerStatement();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InnerStatementContext extends ParserRuleContext {
		public VariableDeclarationContext variableDeclaration() {
			return getRuleContext(VariableDeclarationContext.class,0);
		}
		public VariableReassignmentContext variableReassignment() {
			return getRuleContext(VariableReassignmentContext.class,0);
		}
		public FunctionDeclarationContext functionDeclaration() {
			return getRuleContext(FunctionDeclarationContext.class,0);
		}
		public FunctionInvocationContext functionInvocation() {
			return getRuleContext(FunctionInvocationContext.class,0);
		}
		public ForLoopContext forLoop() {
			return getRuleContext(ForLoopContext.class,0);
		}
		public InnerStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_innerStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterInnerStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitInnerStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitInnerStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InnerStatementContext innerStatement() throws RecognitionException {
		InnerStatementContext _localctx = new InnerStatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_innerStatement);
		try {
			setState(57);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(50);
				variableDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(51);
				variableReassignment();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(52);
				functionDeclaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(53);
				functionInvocation();
				setState(54);
				match(T__0);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(56);
				forLoop();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterDeclarationContext extends ParserRuleContext {
		public TerminalNode PARAMETER_TYPE() { return getToken(ProcModel2Parser.PARAMETER_TYPE, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(ProcModel2Parser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(ProcModel2Parser.IDENTIFIER, i);
		}
		public ParameterDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameterDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterParameterDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitParameterDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitParameterDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParameterDeclarationContext parameterDeclaration() throws RecognitionException {
		ParameterDeclarationContext _localctx = new ParameterDeclarationContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_parameterDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			match(PARAMETER_TYPE);
			setState(60);
			match(T__1);
			setState(61);
			match(IDENTIFIER);
			setState(62);
			match(IDENTIFIER);
			setState(63);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DynamicDeclarationContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(ProcModel2Parser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(ProcModel2Parser.IDENTIFIER, i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<InnerStatementContext> innerStatement() {
			return getRuleContexts(InnerStatementContext.class);
		}
		public InnerStatementContext innerStatement(int i) {
			return getRuleContext(InnerStatementContext.class,i);
		}
		public DynamicDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dynamicDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterDynamicDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitDynamicDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitDynamicDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DynamicDeclarationContext dynamicDeclaration() throws RecognitionException {
		DynamicDeclarationContext _localctx = new DynamicDeclarationContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_dynamicDeclaration);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(65);
			match(T__2);
			setState(66);
			match(IDENTIFIER);
			setState(79);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__3) {
				{
				setState(67);
				match(T__3);
				setState(73);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(68);
						match(IDENTIFIER);
						setState(69);
						match(IDENTIFIER);
						setState(70);
						match(T__4);
						}
						} 
					}
					setState(75);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				}
				setState(76);
				match(IDENTIFIER);
				setState(77);
				match(IDENTIFIER);
				setState(78);
				match(T__5);
				}
			}

			setState(81);
			match(T__6);
			setState(85);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(82);
					innerStatement();
					}
					} 
				}
				setState(87);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			}
			setState(88);
			expression(0);
			setState(89);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableDeclarationContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(ProcModel2Parser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(ProcModel2Parser.IDENTIFIER, i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public VariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclarationContext variableDeclaration() throws RecognitionException {
		VariableDeclarationContext _localctx = new VariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_variableDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(91);
			match(IDENTIFIER);
			setState(92);
			match(IDENTIFIER);
			setState(95);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__8) {
				{
				setState(93);
				match(T__8);
				setState(94);
				expression(0);
				}
			}

			setState(97);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableReassignmentContext extends ParserRuleContext {
		public VariableReassignmentTargetContext variableReassignmentTarget() {
			return getRuleContext(VariableReassignmentTargetContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public VariableReassignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableReassignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterVariableReassignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitVariableReassignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitVariableReassignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableReassignmentContext variableReassignment() throws RecognitionException {
		VariableReassignmentContext _localctx = new VariableReassignmentContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_variableReassignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(99);
			variableReassignmentTarget();
			setState(100);
			match(T__8);
			setState(101);
			expression(0);
			setState(102);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableReassignmentTargetContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(ProcModel2Parser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(ProcModel2Parser.IDENTIFIER, i);
		}
		public VariableReassignmentTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableReassignmentTarget; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterVariableReassignmentTarget(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitVariableReassignmentTarget(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitVariableReassignmentTarget(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableReassignmentTargetContext variableReassignmentTarget() throws RecognitionException {
		VariableReassignmentTargetContext _localctx = new VariableReassignmentTargetContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_variableReassignmentTarget);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(104);
			match(IDENTIFIER);
			setState(109);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__9) {
				{
				{
				setState(105);
				match(T__9);
				setState(106);
				match(IDENTIFIER);
				}
				}
				setState(111);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionDeclarationContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(ProcModel2Parser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(ProcModel2Parser.IDENTIFIER, i);
		}
		public List<InnerStatementContext> innerStatement() {
			return getRuleContexts(InnerStatementContext.class);
		}
		public InnerStatementContext innerStatement(int i) {
			return getRuleContext(InnerStatementContext.class,i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterFunctionDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitFunctionDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitFunctionDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationContext functionDeclaration() throws RecognitionException {
		FunctionDeclarationContext _localctx = new FunctionDeclarationContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_functionDeclaration);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(112);
			match(IDENTIFIER);
			setState(113);
			match(IDENTIFIER);
			setState(114);
			match(T__10);
			setState(125);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IDENTIFIER) {
				{
				setState(120);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(115);
						match(IDENTIFIER);
						setState(116);
						match(IDENTIFIER);
						setState(117);
						match(T__4);
						}
						} 
					}
					setState(122);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
				}
				setState(123);
				match(IDENTIFIER);
				setState(124);
				match(IDENTIFIER);
				}
			}

			setState(127);
			match(T__11);
			setState(128);
			match(T__6);
			setState(132);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(129);
					innerStatement();
					}
					} 
				}
				setState(134);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			}
			setState(136);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 14682120L) != 0)) {
				{
				setState(135);
				expression(0);
				}
			}

			setState(138);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionInvocationContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public FunctionInvocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionInvocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterFunctionInvocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitFunctionInvocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitFunctionInvocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionInvocationContext functionInvocation() throws RecognitionException {
		FunctionInvocationContext _localctx = new FunctionInvocationContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_functionInvocation);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(140);
			match(IDENTIFIER);
			setState(141);
			match(T__10);
			setState(151);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 14682120L) != 0)) {
				{
				setState(147);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(142);
						expression(0);
						setState(143);
						match(T__4);
						}
						} 
					}
					setState(149);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
				}
				setState(150);
				expression(0);
				}
			}

			setState(153);
			match(T__11);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(ProcModel2Parser.FLOAT_LITERAL, 0); }
		public TerminalNode INT_LITERAL() { return getToken(ProcModel2Parser.INT_LITERAL, 0); }
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public FunctionInvocationContext functionInvocation() {
			return getRuleContext(FunctionInvocationContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public PositionConstructorContext positionConstructor() {
			return getRuleContext(PositionConstructorContext.class,0);
		}
		public DynamicDeclarationContext dynamicDeclaration() {
			return getRuleContext(DynamicDeclarationContext.class,0);
		}
		public TerminalNode DIVIDE() { return getToken(ProcModel2Parser.DIVIDE, 0); }
		public TerminalNode TIMES() { return getToken(ProcModel2Parser.TIMES, 0); }
		public TerminalNode MINUS() { return getToken(ProcModel2Parser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(ProcModel2Parser.PLUS, 0); }
		public VariablePropertyContext variableProperty() {
			return getRuleContext(VariablePropertyContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 20;
		enterRecursionRule(_localctx, 20, RULE_expression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(166);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				{
				setState(156);
				match(FLOAT_LITERAL);
				}
				break;
			case 2:
				{
				setState(157);
				match(INT_LITERAL);
				}
				break;
			case 3:
				{
				setState(158);
				match(IDENTIFIER);
				}
				break;
			case 4:
				{
				setState(159);
				functionInvocation();
				}
				break;
			case 5:
				{
				setState(160);
				match(T__10);
				setState(161);
				expression(0);
				setState(162);
				match(T__11);
				}
				break;
			case 6:
				{
				setState(164);
				positionConstructor();
				}
				break;
			case 7:
				{
				setState(165);
				dynamicDeclaration();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(184);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(182);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(168);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(169);
						match(DIVIDE);
						setState(170);
						expression(6);
						}
						break;
					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(171);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(172);
						match(TIMES);
						setState(173);
						expression(5);
						}
						break;
					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(174);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(175);
						match(MINUS);
						setState(176);
						expression(4);
						}
						break;
					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(177);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(178);
						match(PLUS);
						setState(179);
						expression(3);
						}
						break;
					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(180);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(181);
						variableProperty();
						}
						break;
					}
					} 
				}
				setState(186);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariablePropertyContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public VariablePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterVariableProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitVariableProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitVariableProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariablePropertyContext variableProperty() throws RecognitionException {
		VariablePropertyContext _localctx = new VariablePropertyContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_variableProperty);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(187);
			match(T__9);
			setState(188);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PositionConstructorContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public PositionConstructorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_positionConstructor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterPositionConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitPositionConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitPositionConstructor(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PositionConstructorContext positionConstructor() throws RecognitionException {
		PositionConstructorContext _localctx = new PositionConstructorContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_positionConstructor);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(190);
			match(T__10);
			setState(191);
			expression(0);
			setState(192);
			match(T__4);
			setState(193);
			expression(0);
			setState(194);
			match(T__11);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForLoopContext extends ParserRuleContext {
		public ForLoopHeaderContext forLoopHeader() {
			return getRuleContext(ForLoopHeaderContext.class,0);
		}
		public List<InnerStatementContext> innerStatement() {
			return getRuleContexts(InnerStatementContext.class);
		}
		public InnerStatementContext innerStatement(int i) {
			return getRuleContext(InnerStatementContext.class,i);
		}
		public ForLoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterForLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitForLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitForLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopContext forLoop() throws RecognitionException {
		ForLoopContext _localctx = new ForLoopContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_forLoop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(196);
			forLoopHeader();
			setState(197);
			match(T__6);
			setState(201);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__12 || _la==IDENTIFIER) {
				{
				{
				setState(198);
				innerStatement();
				}
				}
				setState(203);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(204);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForLoopHeaderContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ForLoopComparator1Context forLoopComparator1() {
			return getRuleContext(ForLoopComparator1Context.class,0);
		}
		public ForLoopVariableContext forLoopVariable() {
			return getRuleContext(ForLoopVariableContext.class,0);
		}
		public ForLoopComparator2Context forLoopComparator2() {
			return getRuleContext(ForLoopComparator2Context.class,0);
		}
		public ForLoopHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoopHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterForLoopHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitForLoopHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitForLoopHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopHeaderContext forLoopHeader() throws RecognitionException {
		ForLoopHeaderContext _localctx = new ForLoopHeaderContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_forLoopHeader);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			match(T__12);
			setState(207);
			match(T__10);
			setState(208);
			expression(0);
			setState(209);
			forLoopComparator1();
			setState(210);
			forLoopVariable();
			setState(211);
			forLoopComparator2();
			setState(212);
			expression(0);
			setState(213);
			match(T__11);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForLoopVariableContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public ForLoopVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoopVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterForLoopVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitForLoopVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitForLoopVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopVariableContext forLoopVariable() throws RecognitionException {
		ForLoopVariableContext _localctx = new ForLoopVariableContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_forLoopVariable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForLoopComparator1Context extends ParserRuleContext {
		public ForLoopComparatorContext forLoopComparator() {
			return getRuleContext(ForLoopComparatorContext.class,0);
		}
		public ForLoopComparator1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoopComparator1; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterForLoopComparator1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitForLoopComparator1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitForLoopComparator1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopComparator1Context forLoopComparator1() throws RecognitionException {
		ForLoopComparator1Context _localctx = new ForLoopComparator1Context(_ctx, getState());
		enterRule(_localctx, 32, RULE_forLoopComparator1);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			forLoopComparator();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForLoopComparator2Context extends ParserRuleContext {
		public ForLoopComparatorContext forLoopComparator() {
			return getRuleContext(ForLoopComparatorContext.class,0);
		}
		public ForLoopComparator2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoopComparator2; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterForLoopComparator2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitForLoopComparator2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitForLoopComparator2(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopComparator2Context forLoopComparator2() throws RecognitionException {
		ForLoopComparator2Context _localctx = new ForLoopComparator2Context(_ctx, getState());
		enterRule(_localctx, 34, RULE_forLoopComparator2);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
			forLoopComparator();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForLoopComparatorContext extends ParserRuleContext {
		public ForLoopComparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forLoopComparator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterForLoopComparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitForLoopComparator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitForLoopComparator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForLoopComparatorContext forLoopComparator() throws RecognitionException {
		ForLoopComparatorContext _localctx = new ForLoopComparatorContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_forLoopComparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			_la = _input.LA(1);
			if ( !(_la==T__3 || _la==T__13) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 10:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 5);
		case 1:
			return precpred(_ctx, 4);
		case 2:
			return precpred(_ctx, 3);
		case 3:
			return precpred(_ctx, 2);
		case 4:
			return precpred(_ctx, 8);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0018\u00e0\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0001\u0000\u0005\u0000(\b\u0000\n\u0000\f\u0000+\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003\u00011\b\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0003\u0002:\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0005\u0004H\b\u0004\n\u0004\f\u0004K\t"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004P\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0005\u0004T\b\u0004\n\u0004\f\u0004W\t\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0003\u0005`\b\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0005\u0007l\b\u0007\n\u0007\f\u0007o\t\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0005\bw\b\b\n\b\f\bz\t\b\u0001\b\u0001\b\u0003"+
		"\b~\b\b\u0001\b\u0001\b\u0001\b\u0005\b\u0083\b\b\n\b\f\b\u0086\t\b\u0001"+
		"\b\u0003\b\u0089\b\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0005\t\u0092\b\t\n\t\f\t\u0095\t\t\u0001\t\u0003\t\u0098\b\t\u0001"+
		"\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0003\n\u00a7\b\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0005\n\u00b7\b\n\n\n\f\n\u00ba\t\n\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001"+
		"\r\u0001\r\u0005\r\u00c8\b\r\n\r\f\r\u00cb\t\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0000\u0001"+
		"\u0014\u0013\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016"+
		"\u0018\u001a\u001c\u001e \"$\u0000\u0001\u0002\u0000\u0004\u0004\u000e"+
		"\u000e\u00e9\u0000)\u0001\u0000\u0000\u0000\u00020\u0001\u0000\u0000\u0000"+
		"\u00049\u0001\u0000\u0000\u0000\u0006;\u0001\u0000\u0000\u0000\bA\u0001"+
		"\u0000\u0000\u0000\n[\u0001\u0000\u0000\u0000\fc\u0001\u0000\u0000\u0000"+
		"\u000eh\u0001\u0000\u0000\u0000\u0010p\u0001\u0000\u0000\u0000\u0012\u008c"+
		"\u0001\u0000\u0000\u0000\u0014\u00a6\u0001\u0000\u0000\u0000\u0016\u00bb"+
		"\u0001\u0000\u0000\u0000\u0018\u00be\u0001\u0000\u0000\u0000\u001a\u00c4"+
		"\u0001\u0000\u0000\u0000\u001c\u00ce\u0001\u0000\u0000\u0000\u001e\u00d7"+
		"\u0001\u0000\u0000\u0000 \u00d9\u0001\u0000\u0000\u0000\"\u00db\u0001"+
		"\u0000\u0000\u0000$\u00dd\u0001\u0000\u0000\u0000&(\u0003\u0002\u0001"+
		"\u0000\'&\u0001\u0000\u0000\u0000(+\u0001\u0000\u0000\u0000)\'\u0001\u0000"+
		"\u0000\u0000)*\u0001\u0000\u0000\u0000*,\u0001\u0000\u0000\u0000+)\u0001"+
		"\u0000\u0000\u0000,-\u0005\u0000\u0000\u0001-\u0001\u0001\u0000\u0000"+
		"\u0000.1\u0003\u0006\u0003\u0000/1\u0003\u0004\u0002\u00000.\u0001\u0000"+
		"\u0000\u00000/\u0001\u0000\u0000\u00001\u0003\u0001\u0000\u0000\u0000"+
		"2:\u0003\n\u0005\u00003:\u0003\f\u0006\u00004:\u0003\u0010\b\u000056\u0003"+
		"\u0012\t\u000067\u0005\u0001\u0000\u00007:\u0001\u0000\u0000\u00008:\u0003"+
		"\u001a\r\u000092\u0001\u0000\u0000\u000093\u0001\u0000\u0000\u000094\u0001"+
		"\u0000\u0000\u000095\u0001\u0000\u0000\u000098\u0001\u0000\u0000\u0000"+
		":\u0005\u0001\u0000\u0000\u0000;<\u0005\u0014\u0000\u0000<=\u0005\u0002"+
		"\u0000\u0000=>\u0005\u0015\u0000\u0000>?\u0005\u0015\u0000\u0000?@\u0005"+
		"\u0001\u0000\u0000@\u0007\u0001\u0000\u0000\u0000AB\u0005\u0003\u0000"+
		"\u0000BO\u0005\u0015\u0000\u0000CI\u0005\u0004\u0000\u0000DE\u0005\u0015"+
		"\u0000\u0000EF\u0005\u0015\u0000\u0000FH\u0005\u0005\u0000\u0000GD\u0001"+
		"\u0000\u0000\u0000HK\u0001\u0000\u0000\u0000IG\u0001\u0000\u0000\u0000"+
		"IJ\u0001\u0000\u0000\u0000JL\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000"+
		"\u0000LM\u0005\u0015\u0000\u0000MN\u0005\u0015\u0000\u0000NP\u0005\u0006"+
		"\u0000\u0000OC\u0001\u0000\u0000\u0000OP\u0001\u0000\u0000\u0000PQ\u0001"+
		"\u0000\u0000\u0000QU\u0005\u0007\u0000\u0000RT\u0003\u0004\u0002\u0000"+
		"SR\u0001\u0000\u0000\u0000TW\u0001\u0000\u0000\u0000US\u0001\u0000\u0000"+
		"\u0000UV\u0001\u0000\u0000\u0000VX\u0001\u0000\u0000\u0000WU\u0001\u0000"+
		"\u0000\u0000XY\u0003\u0014\n\u0000YZ\u0005\b\u0000\u0000Z\t\u0001\u0000"+
		"\u0000\u0000[\\\u0005\u0015\u0000\u0000\\_\u0005\u0015\u0000\u0000]^\u0005"+
		"\t\u0000\u0000^`\u0003\u0014\n\u0000_]\u0001\u0000\u0000\u0000_`\u0001"+
		"\u0000\u0000\u0000`a\u0001\u0000\u0000\u0000ab\u0005\u0001\u0000\u0000"+
		"b\u000b\u0001\u0000\u0000\u0000cd\u0003\u000e\u0007\u0000de\u0005\t\u0000"+
		"\u0000ef\u0003\u0014\n\u0000fg\u0005\u0001\u0000\u0000g\r\u0001\u0000"+
		"\u0000\u0000hm\u0005\u0015\u0000\u0000ij\u0005\n\u0000\u0000jl\u0005\u0015"+
		"\u0000\u0000ki\u0001\u0000\u0000\u0000lo\u0001\u0000\u0000\u0000mk\u0001"+
		"\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000n\u000f\u0001\u0000\u0000"+
		"\u0000om\u0001\u0000\u0000\u0000pq\u0005\u0015\u0000\u0000qr\u0005\u0015"+
		"\u0000\u0000r}\u0005\u000b\u0000\u0000st\u0005\u0015\u0000\u0000tu\u0005"+
		"\u0015\u0000\u0000uw\u0005\u0005\u0000\u0000vs\u0001\u0000\u0000\u0000"+
		"wz\u0001\u0000\u0000\u0000xv\u0001\u0000\u0000\u0000xy\u0001\u0000\u0000"+
		"\u0000y{\u0001\u0000\u0000\u0000zx\u0001\u0000\u0000\u0000{|\u0005\u0015"+
		"\u0000\u0000|~\u0005\u0015\u0000\u0000}x\u0001\u0000\u0000\u0000}~\u0001"+
		"\u0000\u0000\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0080\u0005\f"+
		"\u0000\u0000\u0080\u0084\u0005\u0007\u0000\u0000\u0081\u0083\u0003\u0004"+
		"\u0002\u0000\u0082\u0081\u0001\u0000\u0000\u0000\u0083\u0086\u0001\u0000"+
		"\u0000\u0000\u0084\u0082\u0001\u0000\u0000\u0000\u0084\u0085\u0001\u0000"+
		"\u0000\u0000\u0085\u0088\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000"+
		"\u0000\u0000\u0087\u0089\u0003\u0014\n\u0000\u0088\u0087\u0001\u0000\u0000"+
		"\u0000\u0088\u0089\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000"+
		"\u0000\u008a\u008b\u0005\b\u0000\u0000\u008b\u0011\u0001\u0000\u0000\u0000"+
		"\u008c\u008d\u0005\u0015\u0000\u0000\u008d\u0097\u0005\u000b\u0000\u0000"+
		"\u008e\u008f\u0003\u0014\n\u0000\u008f\u0090\u0005\u0005\u0000\u0000\u0090"+
		"\u0092\u0001\u0000\u0000\u0000\u0091\u008e\u0001\u0000\u0000\u0000\u0092"+
		"\u0095\u0001\u0000\u0000\u0000\u0093\u0091\u0001\u0000\u0000\u0000\u0093"+
		"\u0094\u0001\u0000\u0000\u0000\u0094\u0096\u0001\u0000\u0000\u0000\u0095"+
		"\u0093\u0001\u0000\u0000\u0000\u0096\u0098\u0003\u0014\n\u0000\u0097\u0093"+
		"\u0001\u0000\u0000\u0000\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u0099"+
		"\u0001\u0000\u0000\u0000\u0099\u009a\u0005\f\u0000\u0000\u009a\u0013\u0001"+
		"\u0000\u0000\u0000\u009b\u009c\u0006\n\uffff\uffff\u0000\u009c\u00a7\u0005"+
		"\u0016\u0000\u0000\u009d\u00a7\u0005\u0017\u0000\u0000\u009e\u00a7\u0005"+
		"\u0015\u0000\u0000\u009f\u00a7\u0003\u0012\t\u0000\u00a0\u00a1\u0005\u000b"+
		"\u0000\u0000\u00a1\u00a2\u0003\u0014\n\u0000\u00a2\u00a3\u0005\f\u0000"+
		"\u0000\u00a3\u00a7\u0001\u0000\u0000\u0000\u00a4\u00a7\u0003\u0018\f\u0000"+
		"\u00a5\u00a7\u0003\b\u0004\u0000\u00a6\u009b\u0001\u0000\u0000\u0000\u00a6"+
		"\u009d\u0001\u0000\u0000\u0000\u00a6\u009e\u0001\u0000\u0000\u0000\u00a6"+
		"\u009f\u0001\u0000\u0000\u0000\u00a6\u00a0\u0001\u0000\u0000\u0000\u00a6"+
		"\u00a4\u0001\u0000\u0000\u0000\u00a6\u00a5\u0001\u0000\u0000\u0000\u00a7"+
		"\u00b8\u0001\u0000\u0000\u0000\u00a8\u00a9\n\u0005\u0000\u0000\u00a9\u00aa"+
		"\u0005\u0012\u0000\u0000\u00aa\u00b7\u0003\u0014\n\u0006\u00ab\u00ac\n"+
		"\u0004\u0000\u0000\u00ac\u00ad\u0005\u0011\u0000\u0000\u00ad\u00b7\u0003"+
		"\u0014\n\u0005\u00ae\u00af\n\u0003\u0000\u0000\u00af\u00b0\u0005\u0010"+
		"\u0000\u0000\u00b0\u00b7\u0003\u0014\n\u0004\u00b1\u00b2\n\u0002\u0000"+
		"\u0000\u00b2\u00b3\u0005\u000f\u0000\u0000\u00b3\u00b7\u0003\u0014\n\u0003"+
		"\u00b4\u00b5\n\b\u0000\u0000\u00b5\u00b7\u0003\u0016\u000b\u0000\u00b6"+
		"\u00a8\u0001\u0000\u0000\u0000\u00b6\u00ab\u0001\u0000\u0000\u0000\u00b6"+
		"\u00ae\u0001\u0000\u0000\u0000\u00b6\u00b1\u0001\u0000\u0000\u0000\u00b6"+
		"\u00b4\u0001\u0000\u0000\u0000\u00b7\u00ba\u0001\u0000\u0000\u0000\u00b8"+
		"\u00b6\u0001\u0000\u0000\u0000\u00b8\u00b9\u0001\u0000\u0000\u0000\u00b9"+
		"\u0015\u0001\u0000\u0000\u0000\u00ba\u00b8\u0001\u0000\u0000\u0000\u00bb"+
		"\u00bc\u0005\n\u0000\u0000\u00bc\u00bd\u0005\u0015\u0000\u0000\u00bd\u0017"+
		"\u0001\u0000\u0000\u0000\u00be\u00bf\u0005\u000b\u0000\u0000\u00bf\u00c0"+
		"\u0003\u0014\n\u0000\u00c0\u00c1\u0005\u0005\u0000\u0000\u00c1\u00c2\u0003"+
		"\u0014\n\u0000\u00c2\u00c3\u0005\f\u0000\u0000\u00c3\u0019\u0001\u0000"+
		"\u0000\u0000\u00c4\u00c5\u0003\u001c\u000e\u0000\u00c5\u00c9\u0005\u0007"+
		"\u0000\u0000\u00c6\u00c8\u0003\u0004\u0002\u0000\u00c7\u00c6\u0001\u0000"+
		"\u0000\u0000\u00c8\u00cb\u0001\u0000\u0000\u0000\u00c9\u00c7\u0001\u0000"+
		"\u0000\u0000\u00c9\u00ca\u0001\u0000\u0000\u0000\u00ca\u00cc\u0001\u0000"+
		"\u0000\u0000\u00cb\u00c9\u0001\u0000\u0000\u0000\u00cc\u00cd\u0005\b\u0000"+
		"\u0000\u00cd\u001b\u0001\u0000\u0000\u0000\u00ce\u00cf\u0005\r\u0000\u0000"+
		"\u00cf\u00d0\u0005\u000b\u0000\u0000\u00d0\u00d1\u0003\u0014\n\u0000\u00d1"+
		"\u00d2\u0003 \u0010\u0000\u00d2\u00d3\u0003\u001e\u000f\u0000\u00d3\u00d4"+
		"\u0003\"\u0011\u0000\u00d4\u00d5\u0003\u0014\n\u0000\u00d5\u00d6\u0005"+
		"\f\u0000\u0000\u00d6\u001d\u0001\u0000\u0000\u0000\u00d7\u00d8\u0005\u0015"+
		"\u0000\u0000\u00d8\u001f\u0001\u0000\u0000\u0000\u00d9\u00da\u0003$\u0012"+
		"\u0000\u00da!\u0001\u0000\u0000\u0000\u00db\u00dc\u0003$\u0012\u0000\u00dc"+
		"#\u0001\u0000\u0000\u0000\u00dd\u00de\u0007\u0000\u0000\u0000\u00de%\u0001"+
		"\u0000\u0000\u0000\u0012)09IOU_mx}\u0084\u0088\u0093\u0097\u00a6\u00b6"+
		"\u00b8\u00c9";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}