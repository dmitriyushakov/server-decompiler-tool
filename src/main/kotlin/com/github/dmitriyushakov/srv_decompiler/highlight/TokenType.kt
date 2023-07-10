package com.github.dmitriyushakov.srv_decompiler.highlight

enum class TokenType {
    Default,
    Keyword,
    PrimitiveType,
    Identifier,
    NumberLiteral,
    StringLiteral,
    BooleanLiteral,
    Operator,
    Spacing,
    Comment
}