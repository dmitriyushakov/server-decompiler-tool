package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.TokenType
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.kvision.core.onClick
import io.kvision.html.Div
import io.kvision.panel.SimplePanel
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.html.customTag
import io.kvision.utils.offsetTop
import web.timers.setTimeout

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

class CodeHighlightView(val codeHighlight: CodeHighlight, val lightedLine: Int?, lastKnownScroll: Double?): SimplePanel("code-highlight-view") {
    var onLinkClicked: ((Path) -> Unit)? = null
    val scrollTop: Double? get() = getElement()?.scrollTop

    init {
        var varLightedLine: Div? = null

        div(className = "code-line-numbers") {
            customTag("pre") {
                for (line in codeHighlight.lines) {
                    val isLighted = line.lineNumber.let { it != null && it == lightedLine }
                    val lineClass = if (isLighted) "code-line-number code-line-number-lighted" else "code-line-number"

                    div(className = lineClass) {
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
                    val isLighted = line.lineNumber.let { it != null && it == lightedLine }
                    val lineClass = if (isLighted) "code-line code-line-lighted" else "code-line"
                    val lineDiv = div(className = lineClass) {
                        for (token in line.tokens) {
                            val tokenTag = span(className = token.tokenType.toClasses(), content = token.text)
                            token.link?.path?.let { path ->
                                tokenTag.onClick {
                                    onLinkClicked?.invoke(path)
                                }
                            }
                        }
                    }
                    if (isLighted) varLightedLine = lineDiv
                }
            }
        }

        val lightedLine = varLightedLine

        if (lightedLine != null) {
            addAfterInsertHook {
                val containerElement = getElement()
                val lightedLineElement = lightedLine.getElement()

                if (containerElement != null && lightedLineElement != null) {
                    val scrollTop = lastKnownScroll ?: containerElement.scrollTop
                    val isOnScreen = (lightedLineElement.offsetTop >= scrollTop) && (lightedLineElement.let { it.offsetTop + it.offsetHeight } <= containerElement.let { scrollTop + it.offsetHeight })
                    val pos: Double = (lightedLineElement.offsetTop + lightedLineElement.offsetHeight / 2 - containerElement.offsetHeight / 2).toDouble()
                    if (!isOnScreen) {
                        containerElement.scroll(0.0, pos)
                    } else {
                        containerElement.scroll(0.0, scrollTop)
                    }
                }
            }
        }
    }
}