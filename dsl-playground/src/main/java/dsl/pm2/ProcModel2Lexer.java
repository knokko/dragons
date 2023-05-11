// Generated from java-escape by ANTLR 4.11.1
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
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, PLUS=13, MINUS=14, TIMES=15, DIVIDE=16, NORMAL_TYPE=17, 
		PARAMETER_TYPE=18, IDENTIFIER=19, FLOAT_LITERAL=20, INT_LITERAL=21, WS=22;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "PLUS", "MINUS", "TIMES", "DIVIDE", "NORMAL_TYPE", 
			"PARAMETER_TYPE", "IDENTIFIER", "FLOAT_LITERAL", "INT_LITERAL", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'parameter'", "'='", "'.'", "'('", "','", "')'", "'{'", 
			"'}'", "'for'", "'<'", "'<='", "'+'", "'-'", "'*'", "'/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, "PLUS", "MINUS", "TIMES", "DIVIDE", "NORMAL_TYPE", "PARAMETER_TYPE", 
			"IDENTIFIER", "FLOAT_LITERAL", "INT_LITERAL", "WS"
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
		"\u0004\u0000\u0016\u0099\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f"+
		"\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010j\b"+
		"\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0003\u0011y\b\u0011\u0001\u0012\u0003\u0012|\b\u0012"+
		"\u0001\u0012\u0005\u0012\u007f\b\u0012\n\u0012\f\u0012\u0082\t\u0012\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0004\u0013\u0087\b\u0013\u000b\u0013\f"+
		"\u0013\u0088\u0001\u0014\u0003\u0014\u008c\b\u0014\u0001\u0014\u0004\u0014"+
		"\u008f\b\u0014\u000b\u0014\f\u0014\u0090\u0001\u0015\u0004\u0015\u0094"+
		"\b\u0015\u000b\u0015\f\u0015\u0095\u0001\u0015\u0001\u0015\u0000\u0000"+
		"\u0016\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006"+
		"\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e"+
		"\u001d\u000f\u001f\u0010!\u0011#\u0012%\u0013\'\u0014)\u0015+\u0016\u0001"+
		"\u0000\u0003\u0002\u0000AZaz\u0003\u000009AZaz\u0003\u0000\t\n\r\r  \u00a0"+
		"\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000"+
		"\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000"+
		"\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000"+
		"\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011"+
		"\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015"+
		"\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019"+
		"\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000\u0000\u001d"+
		"\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000\u0000!\u0001"+
		"\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0000%\u0001\u0000\u0000"+
		"\u0000\u0000\'\u0001\u0000\u0000\u0000\u0000)\u0001\u0000\u0000\u0000"+
		"\u0000+\u0001\u0000\u0000\u0000\u0001-\u0001\u0000\u0000\u0000\u0003/"+
		"\u0001\u0000\u0000\u0000\u00059\u0001\u0000\u0000\u0000\u0007;\u0001\u0000"+
		"\u0000\u0000\t=\u0001\u0000\u0000\u0000\u000b?\u0001\u0000\u0000\u0000"+
		"\rA\u0001\u0000\u0000\u0000\u000fC\u0001\u0000\u0000\u0000\u0011E\u0001"+
		"\u0000\u0000\u0000\u0013G\u0001\u0000\u0000\u0000\u0015K\u0001\u0000\u0000"+
		"\u0000\u0017M\u0001\u0000\u0000\u0000\u0019P\u0001\u0000\u0000\u0000\u001b"+
		"R\u0001\u0000\u0000\u0000\u001dT\u0001\u0000\u0000\u0000\u001fV\u0001"+
		"\u0000\u0000\u0000!i\u0001\u0000\u0000\u0000#x\u0001\u0000\u0000\u0000"+
		"%{\u0001\u0000\u0000\u0000\'\u0083\u0001\u0000\u0000\u0000)\u008b\u0001"+
		"\u0000\u0000\u0000+\u0093\u0001\u0000\u0000\u0000-.\u0005;\u0000\u0000"+
		".\u0002\u0001\u0000\u0000\u0000/0\u0005p\u0000\u000001\u0005a\u0000\u0000"+
		"12\u0005r\u0000\u000023\u0005a\u0000\u000034\u0005m\u0000\u000045\u0005"+
		"e\u0000\u000056\u0005t\u0000\u000067\u0005e\u0000\u000078\u0005r\u0000"+
		"\u00008\u0004\u0001\u0000\u0000\u00009:\u0005=\u0000\u0000:\u0006\u0001"+
		"\u0000\u0000\u0000;<\u0005.\u0000\u0000<\b\u0001\u0000\u0000\u0000=>\u0005"+
		"(\u0000\u0000>\n\u0001\u0000\u0000\u0000?@\u0005,\u0000\u0000@\f\u0001"+
		"\u0000\u0000\u0000AB\u0005)\u0000\u0000B\u000e\u0001\u0000\u0000\u0000"+
		"CD\u0005{\u0000\u0000D\u0010\u0001\u0000\u0000\u0000EF\u0005}\u0000\u0000"+
		"F\u0012\u0001\u0000\u0000\u0000GH\u0005f\u0000\u0000HI\u0005o\u0000\u0000"+
		"IJ\u0005r\u0000\u0000J\u0014\u0001\u0000\u0000\u0000KL\u0005<\u0000\u0000"+
		"L\u0016\u0001\u0000\u0000\u0000MN\u0005<\u0000\u0000NO\u0005=\u0000\u0000"+
		"O\u0018\u0001\u0000\u0000\u0000PQ\u0005+\u0000\u0000Q\u001a\u0001\u0000"+
		"\u0000\u0000RS\u0005-\u0000\u0000S\u001c\u0001\u0000\u0000\u0000TU\u0005"+
		"*\u0000\u0000U\u001e\u0001\u0000\u0000\u0000VW\u0005/\u0000\u0000W \u0001"+
		"\u0000\u0000\u0000XY\u0005c\u0000\u0000YZ\u0005u\u0000\u0000Z[\u0005s"+
		"\u0000\u0000[\\\u0005t\u0000\u0000\\]\u0005o\u0000\u0000]j\u0005m\u0000"+
		"\u0000^_\u0005s\u0000\u0000_`\u0005h\u0000\u0000`a\u0005a\u0000\u0000"+
		"ab\u0005r\u0000\u0000bj\u0005p\u0000\u0000cd\u0005s\u0000\u0000de\u0005"+
		"m\u0000\u0000ef\u0005o\u0000\u0000fg\u0005o\u0000\u0000gh\u0005t\u0000"+
		"\u0000hj\u0005h\u0000\u0000iX\u0001\u0000\u0000\u0000i^\u0001\u0000\u0000"+
		"\u0000ic\u0001\u0000\u0000\u0000j\"\u0001\u0000\u0000\u0000kl\u0005s\u0000"+
		"\u0000lm\u0005t\u0000\u0000mn\u0005a\u0000\u0000no\u0005t\u0000\u0000"+
		"op\u0005i\u0000\u0000py\u0005c\u0000\u0000qr\u0005d\u0000\u0000rs\u0005"+
		"y\u0000\u0000st\u0005n\u0000\u0000tu\u0005a\u0000\u0000uv\u0005m\u0000"+
		"\u0000vw\u0005i\u0000\u0000wy\u0005c\u0000\u0000xk\u0001\u0000\u0000\u0000"+
		"xq\u0001\u0000\u0000\u0000y$\u0001\u0000\u0000\u0000z|\u0007\u0000\u0000"+
		"\u0000{z\u0001\u0000\u0000\u0000|\u0080\u0001\u0000\u0000\u0000}\u007f"+
		"\u0007\u0001\u0000\u0000~}\u0001\u0000\u0000\u0000\u007f\u0082\u0001\u0000"+
		"\u0000\u0000\u0080~\u0001\u0000\u0000\u0000\u0080\u0081\u0001\u0000\u0000"+
		"\u0000\u0081&\u0001\u0000\u0000\u0000\u0082\u0080\u0001\u0000\u0000\u0000"+
		"\u0083\u0084\u0003)\u0014\u0000\u0084\u0086\u0005.\u0000\u0000\u0085\u0087"+
		"\u000209\u0000\u0086\u0085\u0001\u0000\u0000\u0000\u0087\u0088\u0001\u0000"+
		"\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0088\u0089\u0001\u0000"+
		"\u0000\u0000\u0089(\u0001\u0000\u0000\u0000\u008a\u008c\u0005-\u0000\u0000"+
		"\u008b\u008a\u0001\u0000\u0000\u0000\u008b\u008c\u0001\u0000\u0000\u0000"+
		"\u008c\u008e\u0001\u0000\u0000\u0000\u008d\u008f\u000209\u0000\u008e\u008d"+
		"\u0001\u0000\u0000\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u008e"+
		"\u0001\u0000\u0000\u0000\u0090\u0091\u0001\u0000\u0000\u0000\u0091*\u0001"+
		"\u0000\u0000\u0000\u0092\u0094\u0007\u0002\u0000\u0000\u0093\u0092\u0001"+
		"\u0000\u0000\u0000\u0094\u0095\u0001\u0000\u0000\u0000\u0095\u0093\u0001"+
		"\u0000\u0000\u0000\u0095\u0096\u0001\u0000\u0000\u0000\u0096\u0097\u0001"+
		"\u0000\u0000\u0000\u0097\u0098\u0006\u0015\u0000\u0000\u0098,\u0001\u0000"+
		"\u0000\u0000\n\u0000ix{~\u0080\u0088\u008b\u0090\u0095\u0001\u0006\u0000"+
		"\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}