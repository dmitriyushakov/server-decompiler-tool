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

    @Suppress("UNCHECKED_CAST")
    fun <T> put(entity: T, serializer: SequentialFileSerializer<T>): EntityPointer<T> {
        lock.withLock {
            val bytes = serializer.toBytes(this, entity)
            val rafLength = raf.length()

            raf.seek(rafLength)
            raf.write(bytes)

            val pointer = EntityPointer(rafLength, bytes.size, serializer)

            if (entity is SequentialFileSerializable<*>) {
                val serializableEntity = entity as SequentialFileSerializable<T>
                serializableEntity.pointer = pointer
            }

            return pointer
        }
    }

    fun <T: SequentialFileSerializable<T>> put(entity: T): EntityPointer<T> {
        lock.withLock {
            val serializer = entity.serializer
            val bytes = serializer.toBytes(this, entity)
            val rafLength = raf.length()

            raf.seek(rafLength)
            raf.write(bytes)

            val pointer = EntityPointer(rafLength, bytes.size, serializer)
            entity.pointer = pointer
            return pointer
        }
    }

    fun <T: SequentialFileSerializable<T>> get(pointer: EntityPointer<T>): T {
        lock.withLock {
            raf.seek(pointer.offset)
            val bytes = ByteArray(pointer.size)
            raf.read(bytes)

            val entity = pointer.serializer.fromBytes(this, bytes)
            entity.pointer = pointer
            return entity
        }
    }

    fun <T> get(pointer: EntityPointer<T>): T {
        lock.withLock {
            raf.seek(pointer.offset)
            val bytes = ByteArray(pointer.size)
            raf.read(bytes)

            return pointer.serializer.fromBytes(this, bytes)
        }
    }

    override fun close() {
        raf.close()
    }
}