grammar Mal;

compilationUnit: (associations | category | include)* EOF;

// Revisit where meta is placed?
meta: metaType Colon String;
metaType: Info | Rationale | Assumptions;
type: LeftBracket Identifier RightBracket;

include: Include File;

associations: Associations LeftBrace association* RightBrace;
association: Identifier type multiplicity LeftRelation Identifier RightRelation multiplicity type Identifier meta*;
multiplicity: Single | ZeroSingle | SingleMany | Many;

category: Category Identifier meta* LeftBrace asset* RightBrace;

asset: assetType Identifier (Extends Identifier)? meta* LeftBrace attackstep* RightBrace;
assetType: Asset | AbstractAsset;

attackstep: attackstepType Identifier ttc? meta* existence? (reachedType statement (Comma statement)*)?;
attackstepType: AttackstepAll | AttackstepAny | Defense | DefenseExistence | DefenseNonExistence;
ttc: LeftBracket Identifier Parameters? RightBracket;
existence: Existence Identifier (Comma Identifier)*;
reachedType: AttackstepReach | AttackstepInherit;
statement: (Let Identifier Assign)? expr;
expr
    : expr Dot expr
    | LeftParen expr RightParen type?
    | expr (operator expr)+
    | Identifier type? Transitive?;
operator: Intersection | Union;

// LEXER

AttackstepAny: '|';
AttackstepAll: '&';
AttackstepReach: '->';
AttackstepInherit: '+>';

Defense: '#';
DefenseExistence: '3';
DefenseNonExistence: 'E';
Existence: '<-';

LeftRelation: '<--';
RightRelation: '-->';
Single: '1';
ZeroSingle: '0-1';
SingleMany: '1-*';
Many: '*';

LeftParen: '(';
RightParen: ')';
LeftBracket: '[';
RightBracket: ']';
LeftBrace: '{';
RightBrace: '}';
Colon: ':';
Comma: ',';
Dot: '.';
Transitive: '+';
Assign: '=';
Intersection: '/\\';
Union: '\\/';

Associations: 'associations';
Category: 'category';
Info: 'info';
Rationale: 'rationale';
Assumptions: 'assumptions';
Include: 'include';
Asset: 'asset';
AbstractAsset: 'abstractAsset';
Extends: 'extends';
Let: 'let';

Identifier: Letter (Letter | Digit)*;
Parameters: LeftParen Number (Comma Number)* RightParen;
File: (Letter | Digit | Path)+ '.mal';
String: '"' ~["\\]* '"';
fragment Path: [\\/-];
fragment Letter: [a-zA-Z$_];
fragment Number: (Digit* '.')? Digit+;
fragment Digit: [0-9];

Whitespace: [ \t\r\n]+ -> skip;
Comment: '//' ~[\r\n]* -> skip;
