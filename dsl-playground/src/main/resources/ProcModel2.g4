grammar ProcModel2;

start : outerStatement* EOF;

outerStatement :
    parameterDeclaration |
    innerStatement;

innerStatement :
    variableDeclaration |
    variableReassignment |
    functionDeclaration |
    functionInvocation ';' |
    forLoop;

parameterDeclaration : PARAMETER_TYPE 'parameter' IDENTIFIER IDENTIFIER ';';

variableDeclaration : IDENTIFIER IDENTIFIER ('=' expression)? ';';

variableReassignment : variableReassignmentTarget '=' expression ';';

variableReassignmentTarget: IDENTIFIER ('.' IDENTIFIER)*;

functionDeclaration : IDENTIFIER IDENTIFIER '(' ((IDENTIFIER IDENTIFIER ',')* IDENTIFIER IDENTIFIER)? ')' '{' innerStatement* expression? '}';

functionInvocation : IDENTIFIER '(' ((expression ',')* expression)? ')';

expression :
    FLOAT_LITERAL |
    INT_LITERAL |
    IDENTIFIER |
    functionInvocation |
    expression variableProperty |
    '(' expression ')' |
    positionConstructor |
    expression DIVIDE expression |
    expression TIMES expression |
    expression MINUS expression |
    expression PLUS expression;

variableProperty : '.' IDENTIFIER;

positionConstructor : '(' expression ',' expression ')';

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

INT_LITERAL : '-'? ('0'..'9')+;

WS: [ \n\t\r]+ -> skip;
