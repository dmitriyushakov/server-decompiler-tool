package com.github.dmitriyushakov.srv_decompiler.reading_context

import java.io.InputStream

interface ReadingContext {
    fun <R> use(actions: (InputStream) -> R): R

    val readingDataPath: String
}