// Generated from /home/knokko/programming/kotlin/dragons/dsl-playground/src/main/resources/ProcModel2.g4 by ANTLR 4.12.0
package dsl.pm2;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class ProcModel2Lexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, PLUS=15, MINUS=16, TIMES=17, 
		DIVIDE=18, NORMAL_TYPE=19, PARAMETER_TYPE=20, IDENTIFIER=21, FLOAT_LITERAL=22, 
		INT_LITERAL=23, WS=24;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "T__13", "PLUS", "MINUS", "TIMES", 
			"DIVIDE", "NORMAL_TYPE", "PARAMETER_TYPE", "IDENTIFIER", "FLOAT_LITERAL", 
			"INT_LITERAL", "WS"
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


	public ProcModel2Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ProcModel2.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u0018\u00a7\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0003"+
		"\u0012x\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u0087\b\u0013\u0001\u0014\u0003"+
		"\u0014\u008a\b\u0014\u0001\u0014\u0005\u0014\u008d\b\u0014\n\u0014\f\u0014"+
		"\u0090\t\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0004\u0015\u0095\b"+
		"\u0015\u000b\u0015\f\u0015\u0096\u0001\u0016\u0003\u0016\u009a\b\u0016"+
		"\u0001\u0016\u0004\u0016\u009d\b\u0016\u000b\u0016\f\u0016\u009e\u0001"+
		"\u0017\u0004\u0017\u00a2\b\u0017\u000b\u0017\f\u0017\u00a3\u0001\u0017"+
		"\u0001\u0017\u0000\u0000\u0018\u0001\u0001\u0003\u0002\u0005\u0003\u0007"+
		"\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b"+
		"\u0017\f\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012%\u0013"+
		"\'\u0014)\u0015+\u0016-\u0017/\u0018\u0001\u0000\u0003\u0002\u0000AZa"+
		"z\u0003\u000009AZaz\u0003\u0000\t\n\r\r  \u00ae\u0000\u0001\u0001\u0000"+
		"\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000"+
		"\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000"+
		"\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000"+
		"\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000"+
		"\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000"+
		"\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000"+
		"\u0000\u001b\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000"+
		"\u0000\u001f\u0001\u0000\u0000\u0000\u0000!\u0001\u0000\u0000\u0000\u0000"+
		"#\u0001\u0000\u0000\u0000\u0000%\u0001\u0000\u0000\u0000\u0000\'\u0001"+
		"\u0000\u0000\u0000\u0000)\u0001\u0000\u0000\u0000\u0000+\u0001\u0000\u0000"+
		"\u0000\u0000-\u0001\u0000\u0000\u0000\u0000/\u0001\u0000\u0000\u0000\u0001"+
		"1\u0001\u0000\u0000\u0000\u00033\u0001\u0000\u0000\u0000\u0005=\u0001"+
		"\u0000\u0000\u0000\u0007E\u0001\u0000\u0000\u0000\tG\u0001\u0000\u0000"+
		"\u0000\u000bI\u0001\u0000\u0000\u0000\rK\u0001\u0000\u0000\u0000\u000f"+
		"M\u0001\u0000\u0000\u0000\u0011O\u0001\u0000\u0000\u0000\u0013Q\u0001"+
		"\u0000\u0000\u0000\u0015S\u0001\u0000\u0000\u0000\u0017U\u0001\u0000\u0000"+
		"\u0000\u0019W\u0001\u0000\u0000\u0000\u001b[\u0001\u0000\u0000\u0000\u001d"+
		"^\u0001\u0000\u0000\u0000\u001f`\u0001\u0000\u0000\u0000!b\u0001\u0000"+
		"\u0000\u0000#d\u0001\u0000\u0000\u0000%w\u0001\u0000\u0000\u0000\'\u0086"+
		"\u0001\u0000\u0000\u0000)\u0089\u0001\u0000\u0000\u0000+\u0091\u0001\u0000"+
		"\u0000\u0000-\u0099\u0001\u0000\u0000\u0000/\u00a1\u0001\u0000\u0000\u0000"+
		"12\u0005;\u0000\u00002\u0002\u0001\u0000\u0000\u000034\u0005p\u0000\u0000"+
		"45\u0005a\u0000\u000056\u0005r\u0000\u000067\u0005a\u0000\u000078\u0005"+
		"m\u0000\u000089\u0005e\u0000\u00009:\u0005t\u0000\u0000:;\u0005e\u0000"+
		"\u0000;<\u0005r\u0000\u0000<\u0004\u0001\u0000\u0000\u0000=>\u0005d\u0000"+
		"\u0000>?\u0005y\u0000\u0000?@\u0005n\u0000\u0000@A\u0005a\u0000\u0000"+
		"AB\u0005m\u0000\u0000BC\u0005i\u0000\u0000CD\u0005c\u0000\u0000D\u0006"+
		"\u0001\u0000\u0000\u0000EF\u0005<\u0000\u0000F\b\u0001\u0000\u0000\u0000"+
		"GH\u0005,\u0000\u0000H\n\u0001\u0000\u0000\u0000IJ\u0005>\u0000\u0000"+
		"J\f\u0001\u0000\u0000\u0000KL\u0005{\u0000\u0000L\u000e\u0001\u0000\u0000"+
		"\u0000MN\u0005}\u0000\u0000N\u0010\u0001\u0000\u0000\u0000OP\u0005=\u0000"+
		"\u0000P\u0012\u0001\u0000\u0000\u0000QR\u0005.\u0000\u0000R\u0014\u0001"+
		"\u0000\u0000\u0000ST\u0005(\u0000\u0000T\u0016\u0001\u0000\u0000\u0000"+
		"UV\u0005)\u0000\u0000V\u0018\u0001\u0000\u0000\u0000WX\u0005f\u0000\u0000"+
		"XY\u0005o\u0000\u0000YZ\u0005r\u0000\u0000Z\u001a\u0001\u0000\u0000\u0000"+
		"[\\\u0005<\u0000\u0000\\]\u0005=\u0000\u0000]\u001c\u0001\u0000\u0000"+
		"\u0000^_\u0005+\u0000\u0000_\u001e\u0001\u0000\u0000\u0000`a\u0005-\u0000"+
		"\u0000a \u0001\u0000\u0000\u0000bc\u0005*\u0000\u0000c\"\u0001\u0000\u0000"+
		"\u0000de\u0005/\u0000\u0000e$\u0001\u0000\u0000\u0000fg\u0005c\u0000\u0000"+
		"gh\u0005u\u0000\u0000hi\u0005s\u0000\u0000ij\u0005t\u0000\u0000jk\u0005"+
		"o\u0000\u0000kx\u0005m\u0000\u0000lm\u0005s\u0000\u0000mn\u0005h\u0000"+
		"\u0000no\u0005a\u0000\u0000op\u0005r\u0000\u0000px\u0005p\u0000\u0000"+
		"qr\u0005s\u0000\u0000rs\u0005m\u0000\u0000st\u0005o\u0000\u0000tu\u0005"+
		"o\u0000\u0000uv\u0005t\u0000\u0000vx\u0005h\u0000\u0000wf\u0001\u0000"+
		"\u0000\u0000wl\u0001\u0000\u0000\u0000wq\u0001\u0000\u0000\u0000x&\u0001"+
		"\u0000\u0000\u0000yz\u0005s\u0000\u0000z{\u0005t\u0000\u0000{|\u0005a"+
		"\u0000\u0000|}\u0005t\u0000\u0000}~\u0005i\u0000\u0000~\u0087\u0005c\u0000"+
		"\u0000\u007f\u0080\u0005d\u0000\u0000\u0080\u0081\u0005y\u0000\u0000\u0081"+
		"\u0082\u0005n\u0000\u0000\u0082\u0083\u0005a\u0000\u0000\u0083\u0084\u0005"+
		"m\u0000\u0000\u0084\u0085\u0005i\u0000\u0000\u0085\u0087\u0005c\u0000"+
		"\u0000\u0086y\u0001\u0000\u0000\u0000\u0086\u007f\u0001\u0000\u0000\u0000"+
		"\u0087(\u0001\u0000\u0000\u0000\u0088\u008a\u0007\u0000\u0000\u0000\u0089"+
		"\u0088\u0001\u0000\u0000\u0000\u008a\u008e\u0001\u0000\u0000\u0000\u008b"+
		"\u008d\u0007\u0001\u0000\u0000\u008c\u008b\u0001\u0000\u0000\u0000\u008d"+
		"\u0090\u0001\u0000\u0000\u0000\u008e\u008c\u0001\u0000\u0000\u0000\u008e"+
		"\u008f\u0001\u0000\u0000\u0000\u008f*\u0001\u0000\u0000\u0000\u0090\u008e"+
		"\u0001\u0000\u0000\u0000\u0091\u0092\u0003-\u0016\u0000\u0092\u0094\u0005"+
		".\u0000\u0000\u0093\u0095\u000209\u0000\u0094\u0093\u0001\u0000\u0000"+
		"\u0000\u0095\u0096\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000"+
		"\u0000\u0096\u0097\u0001\u0000\u0000\u0000\u0097,\u0001\u0000\u0000\u0000"+
		"\u0098\u009a\u0005-\u0000\u0000\u0099\u0098\u0001\u0000\u0000\u0000\u0099"+
		"\u009a\u0001\u0000\u0000\u0000\u009a\u009c\u0001\u0000\u0000\u0000\u009b"+
		"\u009d\u000209\u0000\u009c\u009b\u0001\u0000\u0000\u0000\u009d\u009e\u0001"+
		"\u0000\u0000\u0000\u009e\u009c\u0001\u0000\u0000\u0000\u009e\u009f\u0001"+
		"\u0000\u0000\u0000\u009f.\u0001\u0000\u0000\u0000\u00a0\u00a2\u0007\u0002"+
		"\u0000\u0000\u00a1\u00a0\u0001\u0000\u0000\u0000\u00a2\u00a3\u0001\u0000"+
		"\u0000\u0000\u00a3\u00a1\u0001\u0000\u0000\u0000\u00a3\u00a4\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a6\u0006\u0017"+
		"\u0000\u0000\u00a60\u0001\u0000\u0000\u0000\n\u0000w\u0086\u0089\u008c"+
		"\u008e\u0096\u0099\u009e\u00a3\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}