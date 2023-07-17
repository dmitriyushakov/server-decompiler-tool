package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType

enum class LinkType(val tokenType: TokenType) {
    Class(TokenType.ClassLink),
    Field(TokenType.FieldLink),
    Method(TokenType.MethodLink),
    LocalVar(TokenType.LocalVarLink)
}