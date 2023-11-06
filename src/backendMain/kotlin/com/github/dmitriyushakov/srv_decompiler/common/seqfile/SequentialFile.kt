package com.github.dmitriyushakov.srv_decompiler.common.seqfile

import com.github.dmitriyushakov.srv_decompiler.common.FilesShutdownActions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.withLock

class SequentialFile(file: File, isTemp: Boolean = true, val compression: Boolean = false): AutoCloseable {
    private val lock: Lock = ReentrantLock()
    private val raf: RandomAccessFile
    constructor(filename: String): this(File(filename))

    init {
        raf = RandomAccessFile(file, "rw")
        FilesShutdownActions.toClose(raf)
        if (isTemp) {
            raf.setLength(0)
            file.deleteOnExit()
            FilesShutdownActions.toDelete(file)
        }
    }

    private fun ByteArray.compress(): ByteArray {
        if (!compression) return this
        val baos = ByteArrayOutputStream()
        val baosGz = GZIPOutputStream(baos)
        baosGz.write(this)
        baosGz.close()
        return baos.toByteArray()
    }

    private fun ByteArray.decompress(): ByteArray {
        if (!compression) return this
        val bain = ByteArrayInputStream(this)
        val bainGz = GZIPInputStream(bain)
        return bainGz.readAllBytes()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> put(entity: T, serializer: SequentialFileSerializer<T>): EntityPointer<T> {
        lock.withLock {
            val bytes = serializer.toBytes(this, entity).compress()
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
            val bytes = serializer.toBytes(this, entity).compress()
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

            val entity = pointer.serializer.fromBytes(this, bytes.decompress())
            entity.pointer = pointer
            return entity
        }
    }

    fun <T> get(pointer: EntityPointer<T>): T {
        lock.withLock {
            raf.seek(pointer.offset)
            val bytes = ByteArray(pointer.size)
            raf.read(bytes)

            return pointer.serializer.fromBytes(this, bytes.decompress())
        }
    }

    override fun close() {
        raf.close()
    }
}