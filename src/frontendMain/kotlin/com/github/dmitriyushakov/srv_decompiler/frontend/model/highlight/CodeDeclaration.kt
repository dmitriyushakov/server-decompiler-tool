package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import kotlinx.serialization.Serializable

@Serializable
data class CodeDeclaration (
    val path: Path,
    val lineNumber: Int
)