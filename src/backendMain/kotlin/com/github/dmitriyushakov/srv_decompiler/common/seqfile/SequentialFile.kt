package com.github.dmitriyushakov.srv_decompiler.common.seqfile

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SequentialFile(file: File, isTemp: Boolean = true): AutoCloseable {
    private val lock: Lock = ReentrantLock()
    private val raf: RandomAccessFile
    constructor(filename: String): this(File(filename))

    init {
        raf = RandomAccessFile(file, "rw")
        if (isTemp) {
            raf.setLength(0)
            file.deleteOnExit()
        }
    }

    fun <T> put(entity: T, serializer: SequentialFileSerializer<T>): EntityPointer<T> {
        lock.withLock {
            val rafLength = raf.length()
            val result = serializer.toBytes(this, rafLength, entity)

            val pointer = result.pointer
            if (pointer != null) return pointer

            val bytes = result.data ?: error("Either pointer or data should be filled!")
            raf.seek(rafLength)
            raf.write(bytes)

            return EntityPointer(rafLength, bytes.size, serializer)
        }
    }

    fun <T> get(pointer: EntityPointer<T>): T {
        lock.withLock {
            raf.seek(pointer.offset)
            val bytes = ByteArray(pointer.size)
            raf.read(bytes)
            return pointer.serializer.fromBytes(this, pointer.offset, bytes)
        }
    }

    override fun close() {
        raf.close()
    }
}