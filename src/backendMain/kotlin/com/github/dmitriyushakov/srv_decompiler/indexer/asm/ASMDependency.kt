package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.DependencyType

class ASMDependency(
    fromPath: List<String>,
    toPath: List<String>,
    override val type: DependencyType
) : Dependency {
    private val fromPathArr: Array<String> = fromPath.toTypedArray()
    private val toPathArr: Array<String> = toPath.toTypedArray()

    override val fromPath: List<String> get() = fromPathArr.toList()
    override val toPath: List<String> get() = toPathArr.toList()
}