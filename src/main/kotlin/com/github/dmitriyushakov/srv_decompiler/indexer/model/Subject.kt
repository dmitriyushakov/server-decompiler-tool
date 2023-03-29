package com.github.dmitriyushakov.srv_decompiler.indexer.model

interface Subject {
    val path: List<String>
    val dependencies: List<Dependency>
    val childrenSubjects: List<Subject>
}