package com.github.dmitriyushakov.srv_decompiler.common.seqfile

interface SequentialFileSerializer<T> {
    fun toBytes(file: SequentialFile, entity: T): ByteArray
    fun fromBytes(file: SequentialFile, bytes: ByteArray): T
}