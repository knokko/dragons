grammar ProcModel2;

start : outerStatement* EOF;

outerStatement :
    parameterDeclaration |
    innerStatement;

innerStatement :
    vertexDeclaration |
    floatDeclaration |
    intDeclaration |
    forLoop;

parameterDeclaration : PARAMETER_TYPE 'parameter' IDENTIFIER ';';

vertexDeclaration : 'vertex' IDENTIFIER '=' '{' 'position' ':' '(' floatExpression ',' floatExpression')' '}' ';';

floatDeclaration : 'float' IDENTIFIER '=' floatExpression ';';

intDeclaration : 'int' IDENTIFIER '=' intExpression ';';

floatExpression :
    FLOAT_LITERAL |
    IDENTIFIER |
    'float' '(' intExpression ')' |
    '(' floatExpression ')' |
    floatExpression TIMES floatExpression |
    floatExpression MINUS floatExpression |
    floatExpression PLUS floatExpression;

intExpression :
    INT_LITERAL |
    IDENTIFIER |
    'int' '(' floatExpression ')' |
    intExpression '+' intExpression;

PLUS : '+';
MINUS : '-';
TIMES : '*';
DIVIDE : '/';

forLoop : 'for' '(' intExpression ('<'|'<=') IDENTIFIER ('<'|'<=') intExpression ')' '{' innerStatement* '}';

NORMAL_TYPE : 'custom' | 'sharp' | 'smooth';

PARAMETER_TYPE : 'static' | 'dynamic';

IDENTIFIER : ('a'..'z') (('a'..'z')|('A'..'Z')|('0'..'9'))*;

FLOAT_LITERAL : INT_LITERAL '.' ('0'..'9')+;

INT_LITERAL : '-'? ('0'..'9')+;

WS: [ \n\t\r]+ -> skip;
