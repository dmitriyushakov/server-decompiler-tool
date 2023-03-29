package com.github.dmitriyushakov.srv_decompiler.reading_context

import java.io.InputStream

interface ReadingContext {
    fun use(actions: (InputStream) -> Unit)
}