grammar Filter;

@header {
package filter;
}

query  : expr EOF ;

expr   : orExpr ;

orExpr : andExpr (OR andExpr)* ;
andExpr: notExpr (AND notExpr)* ;
notExpr: NOT notExpr
       | primary
       ;

primary
    : comparison
    | '(' expr ')'
    ;

comparison
    : IDENTIFIER op=COMPOP value=literal
    | IDENTIFIER IN '(' literalList ')'
    ;

literalList
    : literal (',' literal)*
    ;

literal
    : STRING
    | NUMBER
    ;

COMPOP : '==' | '!=' | '<' | '<=' | '>' | '>=' ;

IN     : 'in';
AND    : 'and';
OR     : 'or';
NOT    : 'not';

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;

STRING : '"' (~["\\] | '\\' .)* '"' ;
NUMBER : [0-9]+ ;

WS : [ \t\r\n]+ -> skip ;
