package com.github.dmitriyushakov.srv_decompiler.highlight

abstract class Token {
    abstract val tokenType: TokenType
    abstract val text: String

    protected fun splitMultilineOrNull(mapping: (String) -> Token): List<Token>? {
        if (text.indexOf('\n') != -1) {
            val splitText = text.split('\n')
            return splitText.map(mapping)
        } else {
            return null
        }
    }
    abstract fun splitMultilineOrNull(): List<Token>?
}