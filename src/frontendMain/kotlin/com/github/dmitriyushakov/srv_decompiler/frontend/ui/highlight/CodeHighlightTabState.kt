package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.DecompilersResponse

class CodeHighlightTabState (
    val decompilers: DecompilersResponse,
    val selectedDecompiler: String
)