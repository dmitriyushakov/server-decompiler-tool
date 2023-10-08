package com.github.dmitriyushakov.srv_decompiler.highlight

class BasicToken(
    override val tokenType: TokenType,
    override val text: String) : Token() {
    override fun splitMultilineOrNull() = splitMultilineOrNull { BasicToken(tokenType, it) }
}