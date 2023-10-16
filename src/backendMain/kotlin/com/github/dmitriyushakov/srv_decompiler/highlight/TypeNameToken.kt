package com.github.dmitriyushakov.srv_decompiler.highlight

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class TypeNameToken(
    override val tokenType: TokenType,
    override val text: String,
    val typePath: Path
) : Token() {
    override fun splitMultilineOrNull() = splitMultilineOrNull { TypeNameToken(tokenType, it, typePath) }
}