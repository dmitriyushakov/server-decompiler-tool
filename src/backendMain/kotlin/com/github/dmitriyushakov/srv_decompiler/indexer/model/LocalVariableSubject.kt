package com.github.dmitriyushakov.srv_decompiler.indexer.model

interface LocalVariableSubject: Subject {
    val owner: MethodSubject
    val name: String
    val descriptor: String
}