package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.LocalVariableSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

class ASMLocalVariableSubject(
    override val owner: MethodSubject,
    override val name: String,
    override val descriptor: String,
    override val path: List<String>,
    override val dependencies: List<Dependency>
) : LocalVariableSubject {
    override val childrenSubjects: List<Subject> get() = emptyList()
}