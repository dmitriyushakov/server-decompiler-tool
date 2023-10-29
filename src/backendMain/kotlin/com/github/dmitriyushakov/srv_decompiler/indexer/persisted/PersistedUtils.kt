package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.indexer.model.*

fun ClassSubject.toPersisted(): PersistedClassSubject = if (this is PersistedClassSubject) this else
    PersistedClassSubject(
        name = name,
        isPublic = isPublic,
        isPrivate = isPrivate,
        isProtected = isProtected,
        isAbstract = isAbstract,
        isInterface = isInterface,
        isFinal = isFinal,
        fields = fields.map { it.toPersisted() },
        methods = methods.map { it.toPersisted() },
        readingContext = readingContext,
        path = path,
        dependencies = dependencies.map { it.toPersisted() },
        sourcePath = sourcePath
    )

fun FieldSubject.toPersisted(): PersistedFieldSubject = if (this is PersistedFieldSubject) this else
    PersistedFieldSubject(
        static = static,
        name = name,
        descriptor = descriptor,
        path = path,
        dependencies = dependencies.map { it.toPersisted() },
        sourcePath = sourcePath
    )

fun MethodSubject.toPersisted(): PersistedMethodSubject = if (this is PersistedMethodSubject) this else
    PersistedMethodSubject(
        static = static,
        name = name,
        descriptor = descriptor,
        localVariableSubject = localVariableSubject.map { it.toPersisted() },
        path = path,
        dependencies = dependencies.map { it.toPersisted() },
        sourcePath = sourcePath
    )

fun LocalVariableSubject.toPersisted(): PersistedLocalVariableSubject = if (this is PersistedLocalVariableSubject) this else
    PersistedLocalVariableSubject(
        name = name,
        descriptor = descriptor,
        path = path,
        dependencies = dependencies.map { it.toPersisted() },
        sourcePath = sourcePath
    )

fun Dependency.toPersisted(): PersistedDependency = if (this is PersistedDependency) this else
    PersistedDependency(
        fromPath = fromPath,
        toPath = toPath,
        type = type
    )