package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import org.objectweb.asm.Opcodes.*

class ASMClassSubject(
    path: List<String>,
    override val name: String,
    val access: Int,
    dependencies: List<Dependency>,
    override val readingContext: ReadingContext,
) : ClassSubject {
    private var fieldsArr: Array<FieldSubject>? = null
    private var methodsArr: Array<MethodSubject>? = null

    private val pathArr: Array<String> = path.toTypedArray()
    private val dependenciesArr: Array<Dependency>? = if (dependencies.isEmpty()) null else dependencies.toTypedArray()
    override val path: List<String> get() = pathArr.toList()
    override val dependencies: List<Dependency> get() = dependenciesArr?.toList() ?: emptyList()
    override val isPublic: Boolean get() = access and ACC_PUBLIC != 0
    override val isPrivate: Boolean get() = access and ACC_PRIVATE != 0
    override val isProtected: Boolean get() = access and ACC_PROTECTED != 0
    override val isAbstract: Boolean get() = access and ACC_ABSTRACT != 0
    override val isInterface: Boolean get() = access and ACC_INTERFACE != 0
    override val isFinal: Boolean get() = access and ACC_FINAL != 0

    override var fields: List<FieldSubject>
        get() = fieldsArr?.toList() ?: emptyList()
        set(value) {
            fieldsArr = value.toTypedArray()
        }
    override var methods: List<MethodSubject>
        get() = methodsArr?.toList() ?: emptyList()
        set(value) {
            methodsArr = value.toTypedArray()
        }
    override val sourcePath: String get() = readingContext.readingDataPath
}