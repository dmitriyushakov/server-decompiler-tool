package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

class ASMFieldSubject(
    override val owner: ClassSubject,
    override val static: Boolean,
    override val name: String,
    override val descriptor: String,
    override val path: List<String>,
    override val dependencies: List<Dependency>
) : FieldSubject {
    override val childrenSubjects: List<Subject> get() = emptyList()
    override val sourcePath: String get() = owner.sourcePath
}