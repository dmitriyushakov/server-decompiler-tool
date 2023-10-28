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
    path: List<String>,
    dependencies: List<Dependency>
) : MethodSubject {
    private var localVariableSubjectArr: Array<LocalVariableSubject>? = null
    private val pathArr: Array<String> = path.toTypedArray()
    private  val dependenciesArr: Array<Dependency>? = if (dependencies.isEmpty()) null else dependencies.toTypedArray()

    override val path: List<String> get() = pathArr.toList()
    override val dependencies: List<Dependency> get() = dependenciesArr?.toList() ?: emptyList()

    override var localVariableSubject: List<LocalVariableSubject>
        get() = localVariableSubjectArr?.toList() ?: emptyList()
        set(value) {
            localVariableSubjectArr = value.toTypedArray()
        }

    override val sourcePath: String get() = owner.sourcePath
}