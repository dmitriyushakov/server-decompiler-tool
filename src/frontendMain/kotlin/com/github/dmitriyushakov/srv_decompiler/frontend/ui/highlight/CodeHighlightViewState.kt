package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight

class CodeHighlightViewState(
    val codeHighlight: CodeHighlight,
    val lightedLine: Int? = null
)