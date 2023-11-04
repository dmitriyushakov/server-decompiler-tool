package com.github.dmitriyushakov.srv_decompiler.common.seqfile

interface SequentialFileSerializer<T> {
    // Offset as function is required there because entities can write
    // each other recursively. The top one will be last because it should
    // contain pointers to child entities. So offset is unknown until his
    // byte buffer is not filled due to entities file grow.
    fun toBytes(file: SequentialFile, offsetGetter: () -> Long, entity: T): Result<T>
    fun fromBytes(file: SequentialFile, offset: Long, bytes: ByteArray): T

    data class Result<T>(val pointer: EntityPointer<T>? = null, val data: ByteArray? = null) {
        init {
            if (pointer == null && data == null) error("Either pointer or data should be filled")
        }
    }

    fun <T> EntityPointer<T>.toResult(): Result<T> = Result(pointer = this)
    fun ByteArray.toResult(): Result<T> = Result(data = this)
}