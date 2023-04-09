grammar ProcModel2;

start : outerStatement* EOF;

outerStatement :
    parameterDeclaration |
    innerStatement;

innerStatement :
    variableDeclaration |
    variableReassignment |
    functionInvocation |
    forLoop;

parameterDeclaration : PARAMETER_TYPE 'parameter' IDENTIFIER ';';

variableDeclaration : IDENTIFIER IDENTIFIER ('=' expression)? ';';

variableReassignment : IDENTIFIER ('.' IDENTIFIER)* '=' expression ';';

functionInvocation : IDENTIFIER '(' ((expression ',')* expression)? ')' ';';

expression :
    FLOAT_LITERAL |
    INT_LITERAL |
    IDENTIFIER |
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

forLoop : 'for' '(' expression ('<'|'<=') IDENTIFIER ('<'|'<=') expression ')' '{' innerStatement* '}';

NORMAL_TYPE : 'custom' | 'sharp' | 'smooth';

PARAMETER_TYPE : 'static' | 'dynamic';

IDENTIFIER : (('a'..'z')|('A'..'Z')) (('a'..'z')|('A'..'Z')|('0'..'9'))*;

FLOAT_LITERAL : INT_LITERAL '.' ('0'..'9')+;

INT_LITERAL : '-'? ('0'..'9')+;

WS: [ \n\t\r]+ -> skip;
