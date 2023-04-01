// Generated from java-escape by ANTLR 4.11.1
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
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, PLUS=17, 
		MINUS=18, TIMES=19, DIVIDE=20, NORMAL_TYPE=21, PARAMETER_TYPE=22, IDENTIFIER=23, 
		FLOAT_LITERAL=24, INT_LITERAL=25, WS=26;
	public static final int
		RULE_start = 0, RULE_outerStatement = 1, RULE_innerStatement = 2, RULE_parameterDeclaration = 3, 
		RULE_vertexDeclaration = 4, RULE_floatDeclaration = 5, RULE_intDeclaration = 6, 
		RULE_floatExpression = 7, RULE_intExpression = 8, RULE_forLoop = 9;
	private static String[] makeRuleNames() {
		return new String[] {
			"start", "outerStatement", "innerStatement", "parameterDeclaration", 
			"vertexDeclaration", "floatDeclaration", "intDeclaration", "floatExpression", 
			"intExpression", "forLoop"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'parameter'", "';'", "'vertex'", "'='", "'{'", "'position'", "':'", 
			"'('", "','", "')'", "'}'", "'float'", "'int'", "'for'", "'<'", "'<='", 
			"'+'", "'-'", "'*'", "'/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, "PLUS", "MINUS", "TIMES", "DIVIDE", "NORMAL_TYPE", 
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
	public String getGrammarFileName() { return "java-escape"; }

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
			setState(23);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((_la) & ~0x3f) == 0 && ((1L << _la) & 4222984L) != 0) {
				{
				{
				setState(20);
				outerStatement();
				}
				}
				setState(25);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(26);
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
			setState(30);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PARAMETER_TYPE:
				enterOuterAlt(_localctx, 1);
				{
				setState(28);
				parameterDeclaration();
				}
				break;
			case T__2:
			case T__11:
			case T__12:
			case T__13:
				enterOuterAlt(_localctx, 2);
				{
				setState(29);
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
		public VertexDeclarationContext vertexDeclaration() {
			return getRuleContext(VertexDeclarationContext.class,0);
		}
		public FloatDeclarationContext floatDeclaration() {
			return getRuleContext(FloatDeclarationContext.class,0);
		}
		public IntDeclarationContext intDeclaration() {
			return getRuleContext(IntDeclarationContext.class,0);
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
			setState(36);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				setState(32);
				vertexDeclaration();
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(33);
				floatDeclaration();
				}
				break;
			case T__12:
				enterOuterAlt(_localctx, 3);
				{
				setState(34);
				intDeclaration();
				}
				break;
			case T__13:
				enterOuterAlt(_localctx, 4);
				{
				setState(35);
				forLoop();
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
	public static class ParameterDeclarationContext extends ParserRuleContext {
		public TerminalNode PARAMETER_TYPE() { return getToken(ProcModel2Parser.PARAMETER_TYPE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
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
			setState(38);
			match(PARAMETER_TYPE);
			setState(39);
			match(T__0);
			setState(40);
			match(IDENTIFIER);
			setState(41);
			match(T__1);
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
	public static class VertexDeclarationContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public List<FloatExpressionContext> floatExpression() {
			return getRuleContexts(FloatExpressionContext.class);
		}
		public FloatExpressionContext floatExpression(int i) {
			return getRuleContext(FloatExpressionContext.class,i);
		}
		public VertexDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_vertexDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterVertexDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitVertexDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitVertexDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VertexDeclarationContext vertexDeclaration() throws RecognitionException {
		VertexDeclarationContext _localctx = new VertexDeclarationContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_vertexDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			match(T__2);
			setState(44);
			match(IDENTIFIER);
			setState(45);
			match(T__3);
			setState(46);
			match(T__4);
			setState(47);
			match(T__5);
			setState(48);
			match(T__6);
			setState(49);
			match(T__7);
			setState(50);
			floatExpression(0);
			setState(51);
			match(T__8);
			setState(52);
			floatExpression(0);
			setState(53);
			match(T__9);
			setState(54);
			match(T__10);
			setState(55);
			match(T__1);
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
	public static class FloatDeclarationContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public FloatExpressionContext floatExpression() {
			return getRuleContext(FloatExpressionContext.class,0);
		}
		public FloatDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floatDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterFloatDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitFloatDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitFloatDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FloatDeclarationContext floatDeclaration() throws RecognitionException {
		FloatDeclarationContext _localctx = new FloatDeclarationContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_floatDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(57);
			match(T__11);
			setState(58);
			match(IDENTIFIER);
			setState(59);
			match(T__3);
			setState(60);
			floatExpression(0);
			setState(61);
			match(T__1);
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
	public static class IntDeclarationContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public IntExpressionContext intExpression() {
			return getRuleContext(IntExpressionContext.class,0);
		}
		public IntDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterIntDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitIntDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitIntDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntDeclarationContext intDeclaration() throws RecognitionException {
		IntDeclarationContext _localctx = new IntDeclarationContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_intDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			match(T__12);
			setState(64);
			match(IDENTIFIER);
			setState(65);
			match(T__3);
			setState(66);
			intExpression(0);
			setState(67);
			match(T__1);
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
	public static class FloatExpressionContext extends ParserRuleContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(ProcModel2Parser.FLOAT_LITERAL, 0); }
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public IntExpressionContext intExpression() {
			return getRuleContext(IntExpressionContext.class,0);
		}
		public List<FloatExpressionContext> floatExpression() {
			return getRuleContexts(FloatExpressionContext.class);
		}
		public FloatExpressionContext floatExpression(int i) {
			return getRuleContext(FloatExpressionContext.class,i);
		}
		public TerminalNode TIMES() { return getToken(ProcModel2Parser.TIMES, 0); }
		public TerminalNode MINUS() { return getToken(ProcModel2Parser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(ProcModel2Parser.PLUS, 0); }
		public FloatExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floatExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterFloatExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitFloatExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitFloatExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FloatExpressionContext floatExpression() throws RecognitionException {
		return floatExpression(0);
	}

	private FloatExpressionContext floatExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		FloatExpressionContext _localctx = new FloatExpressionContext(_ctx, _parentState);
		FloatExpressionContext _prevctx = _localctx;
		int _startState = 14;
		enterRecursionRule(_localctx, 14, RULE_floatExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(81);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FLOAT_LITERAL:
				{
				setState(70);
				match(FLOAT_LITERAL);
				}
				break;
			case IDENTIFIER:
				{
				setState(71);
				match(IDENTIFIER);
				}
				break;
			case T__11:
				{
				setState(72);
				match(T__11);
				setState(73);
				match(T__7);
				setState(74);
				intExpression(0);
				setState(75);
				match(T__9);
				}
				break;
			case T__7:
				{
				setState(77);
				match(T__7);
				setState(78);
				floatExpression(0);
				setState(79);
				match(T__9);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(94);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(92);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
					case 1:
						{
						_localctx = new FloatExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_floatExpression);
						setState(83);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(84);
						match(TIMES);
						setState(85);
						floatExpression(4);
						}
						break;
					case 2:
						{
						_localctx = new FloatExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_floatExpression);
						setState(86);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(87);
						match(MINUS);
						setState(88);
						floatExpression(3);
						}
						break;
					case 3:
						{
						_localctx = new FloatExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_floatExpression);
						setState(89);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(90);
						match(PLUS);
						setState(91);
						floatExpression(2);
						}
						break;
					}
					} 
				}
				setState(96);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
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
	public static class IntExpressionContext extends ParserRuleContext {
		public TerminalNode INT_LITERAL() { return getToken(ProcModel2Parser.INT_LITERAL, 0); }
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
		public FloatExpressionContext floatExpression() {
			return getRuleContext(FloatExpressionContext.class,0);
		}
		public List<IntExpressionContext> intExpression() {
			return getRuleContexts(IntExpressionContext.class);
		}
		public IntExpressionContext intExpression(int i) {
			return getRuleContext(IntExpressionContext.class,i);
		}
		public TerminalNode PLUS() { return getToken(ProcModel2Parser.PLUS, 0); }
		public IntExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).enterIntExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ProcModel2Listener ) ((ProcModel2Listener)listener).exitIntExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ProcModel2Visitor ) return ((ProcModel2Visitor<? extends T>)visitor).visitIntExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntExpressionContext intExpression() throws RecognitionException {
		return intExpression(0);
	}

	private IntExpressionContext intExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		IntExpressionContext _localctx = new IntExpressionContext(_ctx, _parentState);
		IntExpressionContext _prevctx = _localctx;
		int _startState = 16;
		enterRecursionRule(_localctx, 16, RULE_intExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(105);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INT_LITERAL:
				{
				setState(98);
				match(INT_LITERAL);
				}
				break;
			case IDENTIFIER:
				{
				setState(99);
				match(IDENTIFIER);
				}
				break;
			case T__12:
				{
				setState(100);
				match(T__12);
				setState(101);
				match(T__7);
				setState(102);
				floatExpression(0);
				setState(103);
				match(T__9);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(112);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new IntExpressionContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_intExpression);
					setState(107);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(108);
					match(PLUS);
					setState(109);
					intExpression(2);
					}
					} 
				}
				setState(114);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
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
	public static class ForLoopContext extends ParserRuleContext {
		public List<IntExpressionContext> intExpression() {
			return getRuleContexts(IntExpressionContext.class);
		}
		public IntExpressionContext intExpression(int i) {
			return getRuleContext(IntExpressionContext.class,i);
		}
		public TerminalNode IDENTIFIER() { return getToken(ProcModel2Parser.IDENTIFIER, 0); }
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
		enterRule(_localctx, 18, RULE_forLoop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(115);
			match(T__13);
			setState(116);
			match(T__7);
			setState(117);
			intExpression(0);
			setState(118);
			_la = _input.LA(1);
			if ( !(_la==T__14 || _la==T__15) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(119);
			match(IDENTIFIER);
			setState(120);
			_la = _input.LA(1);
			if ( !(_la==T__14 || _la==T__15) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(121);
			intExpression(0);
			setState(122);
			match(T__9);
			setState(123);
			match(T__4);
			setState(127);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((_la) & ~0x3f) == 0 && ((1L << _la) & 28680L) != 0) {
				{
				{
				setState(124);
				innerStatement();
				}
				}
				setState(129);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(130);
			match(T__10);
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
		case 7:
			return floatExpression_sempred((FloatExpressionContext)_localctx, predIndex);
		case 8:
			return intExpression_sempred((IntExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean floatExpression_sempred(FloatExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		case 2:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean intExpression_sempred(IntExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u001a\u0085\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0005\u0000\u0016\b\u0000"+
		"\n\u0000\f\u0000\u0019\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0003\u0001\u001f\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0003\u0002%\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007R\b\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0005\u0007]\b\u0007\n\u0007\f\u0007`\t"+
		"\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b"+
		"\u0003\bj\b\b\u0001\b\u0001\b\u0001\b\u0005\bo\b\b\n\b\f\br\t\b\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0005\t~\b\t\n\t\f\t\u0081\t\t\u0001\t\u0001\t\u0001\t\u0000\u0002"+
		"\u000e\u0010\n\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0000\u0001"+
		"\u0001\u0000\u000f\u0010\u0089\u0000\u0017\u0001\u0000\u0000\u0000\u0002"+
		"\u001e\u0001\u0000\u0000\u0000\u0004$\u0001\u0000\u0000\u0000\u0006&\u0001"+
		"\u0000\u0000\u0000\b+\u0001\u0000\u0000\u0000\n9\u0001\u0000\u0000\u0000"+
		"\f?\u0001\u0000\u0000\u0000\u000eQ\u0001\u0000\u0000\u0000\u0010i\u0001"+
		"\u0000\u0000\u0000\u0012s\u0001\u0000\u0000\u0000\u0014\u0016\u0003\u0002"+
		"\u0001\u0000\u0015\u0014\u0001\u0000\u0000\u0000\u0016\u0019\u0001\u0000"+
		"\u0000\u0000\u0017\u0015\u0001\u0000\u0000\u0000\u0017\u0018\u0001\u0000"+
		"\u0000\u0000\u0018\u001a\u0001\u0000\u0000\u0000\u0019\u0017\u0001\u0000"+
		"\u0000\u0000\u001a\u001b\u0005\u0000\u0000\u0001\u001b\u0001\u0001\u0000"+
		"\u0000\u0000\u001c\u001f\u0003\u0006\u0003\u0000\u001d\u001f\u0003\u0004"+
		"\u0002\u0000\u001e\u001c\u0001\u0000\u0000\u0000\u001e\u001d\u0001\u0000"+
		"\u0000\u0000\u001f\u0003\u0001\u0000\u0000\u0000 %\u0003\b\u0004\u0000"+
		"!%\u0003\n\u0005\u0000\"%\u0003\f\u0006\u0000#%\u0003\u0012\t\u0000$ "+
		"\u0001\u0000\u0000\u0000$!\u0001\u0000\u0000\u0000$\"\u0001\u0000\u0000"+
		"\u0000$#\u0001\u0000\u0000\u0000%\u0005\u0001\u0000\u0000\u0000&\'\u0005"+
		"\u0016\u0000\u0000\'(\u0005\u0001\u0000\u0000()\u0005\u0017\u0000\u0000"+
		")*\u0005\u0002\u0000\u0000*\u0007\u0001\u0000\u0000\u0000+,\u0005\u0003"+
		"\u0000\u0000,-\u0005\u0017\u0000\u0000-.\u0005\u0004\u0000\u0000./\u0005"+
		"\u0005\u0000\u0000/0\u0005\u0006\u0000\u000001\u0005\u0007\u0000\u0000"+
		"12\u0005\b\u0000\u000023\u0003\u000e\u0007\u000034\u0005\t\u0000\u0000"+
		"45\u0003\u000e\u0007\u000056\u0005\n\u0000\u000067\u0005\u000b\u0000\u0000"+
		"78\u0005\u0002\u0000\u00008\t\u0001\u0000\u0000\u00009:\u0005\f\u0000"+
		"\u0000:;\u0005\u0017\u0000\u0000;<\u0005\u0004\u0000\u0000<=\u0003\u000e"+
		"\u0007\u0000=>\u0005\u0002\u0000\u0000>\u000b\u0001\u0000\u0000\u0000"+
		"?@\u0005\r\u0000\u0000@A\u0005\u0017\u0000\u0000AB\u0005\u0004\u0000\u0000"+
		"BC\u0003\u0010\b\u0000CD\u0005\u0002\u0000\u0000D\r\u0001\u0000\u0000"+
		"\u0000EF\u0006\u0007\uffff\uffff\u0000FR\u0005\u0018\u0000\u0000GR\u0005"+
		"\u0017\u0000\u0000HI\u0005\f\u0000\u0000IJ\u0005\b\u0000\u0000JK\u0003"+
		"\u0010\b\u0000KL\u0005\n\u0000\u0000LR\u0001\u0000\u0000\u0000MN\u0005"+
		"\b\u0000\u0000NO\u0003\u000e\u0007\u0000OP\u0005\n\u0000\u0000PR\u0001"+
		"\u0000\u0000\u0000QE\u0001\u0000\u0000\u0000QG\u0001\u0000\u0000\u0000"+
		"QH\u0001\u0000\u0000\u0000QM\u0001\u0000\u0000\u0000R^\u0001\u0000\u0000"+
		"\u0000ST\n\u0003\u0000\u0000TU\u0005\u0013\u0000\u0000U]\u0003\u000e\u0007"+
		"\u0004VW\n\u0002\u0000\u0000WX\u0005\u0012\u0000\u0000X]\u0003\u000e\u0007"+
		"\u0003YZ\n\u0001\u0000\u0000Z[\u0005\u0011\u0000\u0000[]\u0003\u000e\u0007"+
		"\u0002\\S\u0001\u0000\u0000\u0000\\V\u0001\u0000\u0000\u0000\\Y\u0001"+
		"\u0000\u0000\u0000]`\u0001\u0000\u0000\u0000^\\\u0001\u0000\u0000\u0000"+
		"^_\u0001\u0000\u0000\u0000_\u000f\u0001\u0000\u0000\u0000`^\u0001\u0000"+
		"\u0000\u0000ab\u0006\b\uffff\uffff\u0000bj\u0005\u0019\u0000\u0000cj\u0005"+
		"\u0017\u0000\u0000de\u0005\r\u0000\u0000ef\u0005\b\u0000\u0000fg\u0003"+
		"\u000e\u0007\u0000gh\u0005\n\u0000\u0000hj\u0001\u0000\u0000\u0000ia\u0001"+
		"\u0000\u0000\u0000ic\u0001\u0000\u0000\u0000id\u0001\u0000\u0000\u0000"+
		"jp\u0001\u0000\u0000\u0000kl\n\u0001\u0000\u0000lm\u0005\u0011\u0000\u0000"+
		"mo\u0003\u0010\b\u0002nk\u0001\u0000\u0000\u0000or\u0001\u0000\u0000\u0000"+
		"pn\u0001\u0000\u0000\u0000pq\u0001\u0000\u0000\u0000q\u0011\u0001\u0000"+
		"\u0000\u0000rp\u0001\u0000\u0000\u0000st\u0005\u000e\u0000\u0000tu\u0005"+
		"\b\u0000\u0000uv\u0003\u0010\b\u0000vw\u0007\u0000\u0000\u0000wx\u0005"+
		"\u0017\u0000\u0000xy\u0007\u0000\u0000\u0000yz\u0003\u0010\b\u0000z{\u0005"+
		"\n\u0000\u0000{\u007f\u0005\u0005\u0000\u0000|~\u0003\u0004\u0002\u0000"+
		"}|\u0001\u0000\u0000\u0000~\u0081\u0001\u0000\u0000\u0000\u007f}\u0001"+
		"\u0000\u0000\u0000\u007f\u0080\u0001\u0000\u0000\u0000\u0080\u0082\u0001"+
		"\u0000\u0000\u0000\u0081\u007f\u0001\u0000\u0000\u0000\u0082\u0083\u0005"+
		"\u000b\u0000\u0000\u0083\u0013\u0001\u0000\u0000\u0000\t\u0017\u001e$"+
		"Q\\^ip\u007f";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}