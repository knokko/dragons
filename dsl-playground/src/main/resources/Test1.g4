grammar Test1;

start : expression EOF;

expression :
    INT |
    expression '+' expression |
    expression '-' expression |
    expression '/' expression |
    expression '*' expression;

INT: '0'..'9'+;
