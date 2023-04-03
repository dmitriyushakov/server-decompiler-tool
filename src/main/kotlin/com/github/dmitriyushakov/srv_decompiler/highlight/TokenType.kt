package com.github.dmitriyushakov.srv_decompiler.highlight

enum class TokenType {
    Default,
    Keyword,
    TypeName,
    MethodName,
    FieldName,
    LocalVariableName,
    NumberLiteral,
    StringLiteral,
    Annotation,
    Spacing
}