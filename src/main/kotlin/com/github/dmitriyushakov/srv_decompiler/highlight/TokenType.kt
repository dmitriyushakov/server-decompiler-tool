package com.github.dmitriyushakov.srv_decompiler.highlight

enum class TokenType {
    Default,
    Keyword,
    TypeName,
    PrimitiveType,
    MethodName,
    FieldName,
    LocalVariableName,
    NumberLiteral,
    StringLiteral,
    BooleanLiteral,
    Annotation,
    Spacing,
    Comment
}