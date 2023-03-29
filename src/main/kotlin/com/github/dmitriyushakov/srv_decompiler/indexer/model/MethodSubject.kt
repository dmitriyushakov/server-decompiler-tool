package com.github.dmitriyushakov.srv_decompiler.indexer.model

interface MethodSubject: Subject {
    val owner: ClassSubject
    val name: String
    val descriptor: String
    val localVariableSubject: List<LocalVariableSubject>

    override val childrenSubjects: List<Subject> get() = localVariableSubject
}