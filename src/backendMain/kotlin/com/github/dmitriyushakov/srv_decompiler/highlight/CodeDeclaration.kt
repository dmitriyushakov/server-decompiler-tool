package com.github.dmitriyushakov.srv_decompiler.highlight

import com.github.dmitriyushakov.srv_decompiler.registry.Path

data class CodeDeclaration (
    val path: Path,
    val lineNumber: Int
)