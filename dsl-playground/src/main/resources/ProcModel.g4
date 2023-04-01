grammar ProcModel;

start : outerStatement* EOF;

outerStatement :
    parameterDeclaration |
    innerStatement;

innerStatement :
    vertexDeclaration;

parameterDeclaration :
    PARAMETER_TYPE 'parameter' IDENTIFIER ';';

vertexDeclaration :
    'vertex' IDENTIFIER '=' '{' 'position' ':' '(' expression ',' expression ',' expression ')' ',' 'normal' ':' NORMAL_TYPE '}' ';';

expression :
    FLOAT_LITERAL |
    INT_LITERAL |
    expression '+' expression;

NORMAL_TYPE : 'custom' | 'sharp' | 'smooth';

PARAMETER_TYPE : 'constant' | 'static' | 'dynamic';

IDENTIFIER : ('a'..'z') (('a'..'z')|('A'..'Z')|('0'..'9'))*;

FLOAT_LITERAL : INT_LITERAL '.' ('0'..'9')+;

INT_LITERAL : '-'? ('0'..'9')+;

WS: [ \n\t\r]+ -> skip;
