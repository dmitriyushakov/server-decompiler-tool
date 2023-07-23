package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

import kotlinx.serialization.Serializable

@Serializable
data class CodeLine (val lineNumber: Int?, val tokens: List<Token>)