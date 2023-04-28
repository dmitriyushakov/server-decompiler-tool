package com.github.dmitriyushakov.srv_decompiler.utils.highlight

import com.github.dmitriyushakov.srv_decompiler.highlight.BasicToken
import com.github.dmitriyushakov.srv_decompiler.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.highlight.CodeLine
import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType

fun plainTextToHighlight(text: String): CodeHighlight =
    text
        .split('\n')
        .map { BasicToken(TokenType.Default, it) }
        .map { CodeLine(listOf(it)) }
        .let(::CodeHighlight)