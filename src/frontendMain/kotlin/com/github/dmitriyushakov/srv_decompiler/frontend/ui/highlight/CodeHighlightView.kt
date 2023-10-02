package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.TokenType
import io.kvision.panel.SimplePanel
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.html.customTag

private val tokenTypeClassesSuffixMap: Map<TokenType, String> = mapOf(
    TokenType.Keyword to "keyword",
    TokenType.PrimitiveType to "primitive-type",
    TokenType.Identifier to "identifier",
    TokenType.NumberLiteral to "number-literal",
    TokenType.StringLiteral to "string-literal",
    TokenType.BooleanLiteral to "boolean-literal",
    TokenType.Operator to "operator",
    TokenType.Spacing to "spacing",
    TokenType.Comment to "comment",
    TokenType.ClassLink to "class-link",
    TokenType.FieldLink to "field-link",
    TokenType.MethodLink to "method-link",
    TokenType.LocalVarLink to "local-var-link"
)

private fun TokenType.toClasses(): String = tokenTypeClassesSuffixMap.get(this).let { suffix ->
    if (suffix == null) "code-line-token" else "code-line-token code-line-token-$suffix"
}

class CodeHighlightView(val codeHighlight: CodeHighlight): SimplePanel("code-highlight-view") {
    init {
        div(className = "code-line-numbers") {
            customTag("pre") {
                for (line in codeHighlight.lines) {
                    div(className = "code-line-number") {
                        line.lineNumber?.let {
                            +it.toString()
                        }
                    }
                }
            }
        }
        div(className = "code-lines") {
            customTag("pre") {
                for (line in codeHighlight.lines) {
                    div(className = "code-line") {
                        for (token in line.tokens) {
                            token.link?.linkType
                            span(className = token.tokenType.toClasses(), content = token.text)
                        }
                    }
                }
            }
        }
    }
}