package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

enum class LinkType(val tokenType: TokenType) {
    Class(TokenType.ClassLink),
    Field(TokenType.FieldLink),
    Method(TokenType.MethodLink),
    LocalVar(TokenType.LocalVarLink)
}