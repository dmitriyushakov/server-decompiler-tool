package com.github.dmitriyushakov.srv_decompiler.indexer.model

import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext

interface ClassSubject: Subject {
    val name: String
    val isPublic: Boolean
    val isPrivate: Boolean
    val isProtected: Boolean
    val isAbstract: Boolean
    val isInterface: Boolean
    val isFinal: Boolean

    val fields: List<FieldSubject>
    val methods: List<MethodSubject>

    override val childrenSubjects: List<Subject> get() = sequenceOf(fields, methods).flatten().toList()
    val readingContext: ReadingContext
}