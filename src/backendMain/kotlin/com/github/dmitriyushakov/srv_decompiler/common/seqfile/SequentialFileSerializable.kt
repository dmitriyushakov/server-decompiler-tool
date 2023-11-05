package com.github.dmitriyushakov.srv_decompiler.common.seqfile

interface SequentialFileSerializable<T> {
    var pointer: EntityPointer<T>?
    val serializer: SequentialFileSerializer<T>
}