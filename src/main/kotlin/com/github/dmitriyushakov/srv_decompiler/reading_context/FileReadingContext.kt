package com.github.dmitriyushakov.srv_decompiler.reading_context

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class FileReadingContext(val file: File): ReadingContext {
    constructor(filename: String): this(File(filename))

    override fun use(actions: (InputStream) -> Unit) {
        FileInputStream(file).use { fileInputStream ->
            BufferedInputStream(fileInputStream).use(actions)
        }
    }
}