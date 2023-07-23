package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

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
    Comment,
    ClassLink,
    FieldLink,
    MethodLink,
    LocalVarLink
}