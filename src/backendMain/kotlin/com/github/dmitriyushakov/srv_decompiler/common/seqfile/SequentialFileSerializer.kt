package com.github.dmitriyushakov.srv_decompiler.common.seqfile

interface SequentialFileSerializer<T> {
    fun toBytes(file: SequentialFile, offset: Long, entity: T): Result<T>
    fun fromBytes(file: SequentialFile, offset: Long, bytes: ByteArray): T

    data class Result<T>(val pointer: EntityPointer<T>? = null, val data: ByteArray? = null) {
        init {
            if (pointer == null && data == null) error("Either pointer or data should be filled")
        }
    }

    fun <T> EntityPointer<T>.toResult(): Result<T> = Result(pointer = this)
    fun ByteArray.toResult(): Result<T> = Result(data = this)
}