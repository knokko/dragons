grammar ProcModel2;

start : outerStatement* EOF;

outerStatement :
    parameterDeclaration |
    importModel |
    importValue |
    innerStatement;

innerStatement :
    variableDeclaration |
    variableReassignment |
    functionDeclaration |
    childModel |
    functionInvocation ';' |
    updateArrayOrMap |
    forLoop;

relativeImportPrefix: './';

relativeImportPath: (IDENTIFIER '/')* IDENTIFIER;

importPath: relativeImportPrefix? relativeImportPath;

importModel: 'import' 'model' importPath importAlias? ';';

importAlias: 'as' IDENTIFIER;

importValue: 'import' IDENTIFIER 'value' importPath importAlias? ';';

childModel: 'child' 'model' IDENTIFIER '(' expression ',' expression ')' ';';

parameterDeclaration : PARAMETER_TYPE 'parameter' IDENTIFIER IDENTIFIER ';';

dynamicDeclaration: 'dynamic' IDENTIFIER ('<' (IDENTIFIER IDENTIFIER ',')* IDENTIFIER IDENTIFIER '>')? '{' innerStatement* expression '}';

variableDeclaration : IDENTIFIER IDENTIFIER ('=' expression)? ';';

variableReassignment : variableReassignmentTarget '=' expression ';';

variableReassignmentTarget: IDENTIFIER ('.' IDENTIFIER)*;

functionDeclaration : IDENTIFIER IDENTIFIER '(' ((IDENTIFIER IDENTIFIER ',')* IDENTIFIER IDENTIFIER)? ')' '{' innerStatement* expression? '}';

functionInvocation : IDENTIFIER '(' ((expression ',')* expression)? ')';

readArrayOrMap: '[' expression ']';

updateArrayOrMap: expression '[' expression ']' '=' expression ';';

expression :
    FLOAT_LITERAL |
    INT_LITERAL |
    STRING_LITERAL |
    IDENTIFIER |
    functionInvocation |
    expression variableProperty |
    expression readArrayOrMap |
    '(' expression ')' |
    positionConstructor |
    listDeclaration |
    expression DIVIDE expression |
    expression TIMES expression |
    expression MINUS expression |
    expression PLUS expression |
    dynamicDeclaration;

variableProperty : '.' IDENTIFIER;

positionConstructor : '(' expression ',' expression ')';

listElement : expression;

listDeclaration : '[' (listElement ',')* listElement? ']';

PLUS : '+';
MINUS : '-';
TIMES : '*';
DIVIDE : '/';

forLoop : forLoopHeader '{' innerStatement* '}';

forLoopHeader : 'for' '(' expression forLoopComparator1 forLoopVariable forLoopComparator2 expression ')';

forLoopVariable : IDENTIFIER;

forLoopComparator1 : forLoopComparator;

forLoopComparator2 : forLoopComparator;

forLoopComparator : '<' | '<=';

NORMAL_TYPE : 'custom' | 'sharp' | 'smooth';

PARAMETER_TYPE : 'static' | 'dynamic';

IDENTIFIER : (('a'..'z')|('A'..'Z')) (('a'..'z')|('A'..'Z')|('0'..'9'))*;

FLOAT_LITERAL : INT_LITERAL '.' ('0'..'9')+;

STRING_LITERAL : '"' .*? '"';

INT_LITERAL : '-'? ('0'..'9')+;

WS: [ \n\t\r]+ -> skip;
