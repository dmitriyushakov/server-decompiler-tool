package com.github.dmitriyushakov.srv_decompiler.highlight

import com.github.dmitriyushakov.srv_decompiler.highlight.scope.Link

class LinkedToken private constructor(
    override val tokenType: TokenType,
    override val text: String,
    val link: Link
) : Token() {
    constructor(text: String, link: Link): this(link.linkType.tokenType, text, link)

    override fun splitMultilineOrNull() = splitMultilineOrNull { LinkedToken(tokenType, it, link) }
}