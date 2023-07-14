package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.LocalVariableSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject

class ASMMethodSubject(
    override val owner: ClassSubject,
    override val static: Boolean,
    override val name: String,
    override val descriptor: String,
    override val path: List<String>,
    override val dependencies: List<Dependency>
) : MethodSubject {
    override val localVariableSubject: MutableList<LocalVariableSubject> = mutableListOf()
}