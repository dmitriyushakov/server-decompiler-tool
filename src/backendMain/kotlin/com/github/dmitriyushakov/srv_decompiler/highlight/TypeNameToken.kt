package com.github.dmitriyushakov.srv_decompiler.highlight

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class TypeNameToken(
    override val tokenType: TokenType,
    override val text: String,
    val links: List<LinkGroup>
) : Token() {
    fun getLinksOfType(linkGroupType: LinkGroupType): List<Path> =
        links
        .firstOrNull { it.groupType == linkGroupType }
        ?.pathLinks
        ?:emptyList()

    override fun splitMultilineOrNull() = splitMultilineOrNull { TypeNameToken(tokenType, it, links) }
}