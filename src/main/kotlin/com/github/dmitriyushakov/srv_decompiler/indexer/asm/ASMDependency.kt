package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.DependencyType

class ASMDependency(
    override val fromPath: List<String>,
    override val toPath: List<String>,
    override val type: DependencyType
) : Dependency