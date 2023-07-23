package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val path: Path,
    val topLevelClassPath: Path,
    val linkType: LinkType,
    val lineNumber: Int? = null
)