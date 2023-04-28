package com.github.dmitriyushakov.srv_decompiler.utils.highlight.parse

import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType
import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType.*
import com.github.javaparser.JavaToken
import com.github.javaparser.JavaToken.Kind.*

fun JavaToken.toTokenType(): TokenType = when(valueOf(kind)) {
    SPACE -> Spacing
    SINGLE_LINE_COMMENT,
    ENTER_JAVADOC_COMMENT,
    ENTER_MULTILINE_COMMENT,
    JAVADOC_COMMENT,
    MULTI_LINE_COMMENT,
    COMMENT_CONTENT -> Comment
    ABSTRACT,
    ASSERT,
    BREAK,
    CASE,
    CATCH,
    CLASS,
    CONST,
    CONTINUE,
    _DEFAULT,
    DO,
    ELSE,
    ENUM,
    EXTENDS,
    FINAL,
    FINALLY,
    FOR,
    GOTO,
    IF,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INTERFACE,
    NATIVE,
    NEW,
    NULL,
    PACKAGE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RECORD,
    RETURN,
    STATIC,
    STRICTFP,
    SUPER,
    SWITCH,
    SYNCHRONIZED,
    THIS,
    THROW,
    THROWS,
    TRANSIENT,
    TRY,
    VOLATILE,
    WHILE,
    YIELD,
    REQUIRES,
    TO,
    WITH,
    OPEN,
    OPENS,
    USES,
    MODULE,
    EXPORTS,
    PROVIDES,
    TRANSITIVE -> Keyword
    BOOLEAN,
    BYTE,
    CHAR,
    DOUBLE,
    FLOAT,
    INT,
    LONG,
    SHORT,
    VOID -> PrimitiveType
    FALSE,
    TRUE -> BooleanLiteral
    LONG_LITERAL,
    INTEGER_LITERAL,
    DECIMAL_LITERAL,
    HEX_LITERAL,
    OCTAL_LITERAL,
    BINARY_LITERAL,
    FLOATING_POINT_LITERAL,
    DECIMAL_FLOATING_POINT_LITERAL,
    DECIMAL_EXPONENT,
    HEXADECIMAL_FLOATING_POINT_LITERAL,
    HEXADECIMAL_EXPONENT,
    HEX_DIGITS -> NumberLiteral
    UNICODE_ESCAPE,
    CHARACTER_LITERAL,
    STRING_LITERAL,
    ENTER_TEXT_BLOCK,
    TEXT_BLOCK_LITERAL,
    TEXT_BLOCK_CONTENT -> StringLiteral
    else -> Default
}

val JavaToken.isEndOfLine: Boolean get() = when(valueOf(kind)) {
    WINDOWS_EOL, UNIX_EOL, OLD_MAC_EOL -> true
    else -> false
}