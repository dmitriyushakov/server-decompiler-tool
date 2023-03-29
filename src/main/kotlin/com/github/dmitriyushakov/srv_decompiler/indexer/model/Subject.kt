package com.github.dmitriyushakov.srv_decompiler.indexer.model

import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext

interface Subject {
    val path: List<String>
    val dependencies: List<Dependency>
    val childrenSubjects: List<Subject>
    val readingContext: ReadingContext
}