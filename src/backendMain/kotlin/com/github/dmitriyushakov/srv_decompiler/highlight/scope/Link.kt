package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.findTopLevelClassPath

data class Link(
    val path: Path,
    val topLevelClassPath: Path,
    val linkType: LinkType,
    val lineNumber: Int? = null
) {
    companion object {
        fun fromPath(path: Path, linkType: LinkType, lineNumber: Int? = null): Link? =
            globalIndexRegistry.subjectsIndex.findTopLevelClassPath(path)?.let { topLevelClassPath ->
                Link(path, topLevelClassPath, linkType, lineNumber)
            }
    }
}