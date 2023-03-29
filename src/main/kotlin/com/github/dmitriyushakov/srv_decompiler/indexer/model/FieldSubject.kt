package com.github.dmitriyushakov.srv_decompiler.indexer.model

interface FieldSubject: Subject {
    val owner: ClassSubject
    val name: String
    val descriptor: String
}