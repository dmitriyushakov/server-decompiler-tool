package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

import kotlinx.serialization.Serializable

@Serializable
data class CodeHighlight (
    val lines: List<CodeLine>,
    val declarations: List<CodeDeclaration>
) {
    companion object {
        val empty = CodeHighlight(emptyList(), emptyList())
    }
}