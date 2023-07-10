package com.github.dmitriyushakov.srv_decompiler.utils.highlight

import com.github.dmitriyushakov.srv_decompiler.exception.JavaParserProblemsException
import com.github.dmitriyushakov.srv_decompiler.highlight.BasicToken
import com.github.dmitriyushakov.srv_decompiler.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.highlight.CodeLine
import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType
import com.github.dmitriyushakov.srv_decompiler.utils.highlight.parse.javaSourceToHighlight
import io.ktor.util.logging.*
import org.slf4j.LoggerFactory

private class HighlightUtils

private val logger = LoggerFactory.getLogger(HighlightUtils::class.java)

fun plainTextToHighlight(text: String): CodeHighlight =
    text
        .split('\n')
        .map { BasicToken(TokenType.Default, it) }
        .map { CodeLine(listOf(it)) }
        .let(::CodeHighlight)

fun sourceToHighlight(text: String): CodeHighlight {
    try {
        return javaSourceToHighlight(text)
    } catch (ex: JavaParserProblemsException) {
        logger.error(ex)

        // Fallback
        return plainTextToHighlight(text)
    }
}