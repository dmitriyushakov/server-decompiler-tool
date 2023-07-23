package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import org.objectweb.asm.Opcodes.*

class ASMClassSubject(
    override val path: List<String>,
    override val name: String,
    val access: Int,
    override val dependencies: List<Dependency>,
    override val readingContext: ReadingContext,
) : ClassSubject {
    override val isPublic: Boolean get() = access and ACC_PUBLIC != 0
    override val isPrivate: Boolean get() = access and ACC_PRIVATE != 0
    override val isProtected: Boolean get() = access and ACC_PROTECTED != 0
    override val isAbstract: Boolean get() = access and ACC_ABSTRACT != 0
    override val isInterface: Boolean get() = access and ACC_INTERFACE != 0
    override val isFinal: Boolean get() = access and ACC_FINAL != 0

    override val fields: MutableList<FieldSubject> = mutableListOf()
    override val methods: MutableList<MethodSubject> = mutableListOf()
    override val sourcePath: String get() = readingContext.readingDataPath
}