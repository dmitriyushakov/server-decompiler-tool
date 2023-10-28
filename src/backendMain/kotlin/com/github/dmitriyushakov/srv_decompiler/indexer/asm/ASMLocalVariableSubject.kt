package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.LocalVariableSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

class ASMLocalVariableSubject(
    override val owner: MethodSubject,
    override val name: String,
    override val descriptor: String,
    path: List<String>,
    dependencies: List<Dependency>
) : LocalVariableSubject {
    private val pathArr: Array<String> = path.toTypedArray()
    private  val dependenciesArr: Array<Dependency>? = if (dependencies.isEmpty()) null else dependencies.toTypedArray()

    override val path: List<String> get() = pathArr.toList()
    override val dependencies: List<Dependency> get() = dependenciesArr?.toList() ?: emptyList()

    override val childrenSubjects: List<Subject> get() = emptyList()
    override val sourcePath: String get() = owner.sourcePath
}