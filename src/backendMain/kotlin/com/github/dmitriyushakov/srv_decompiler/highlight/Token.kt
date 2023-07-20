package com.github.dmitriyushakov.srv_decompiler.highlight

abstract class Token {
    abstract val tokenType: TokenType
    abstract val text: String
}