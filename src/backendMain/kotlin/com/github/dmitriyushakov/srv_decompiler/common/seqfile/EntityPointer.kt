package com.github.dmitriyushakov.srv_decompiler.common.seqfile

class EntityPointer<T>(
    val offset: Long,
    val size: Int,
    val serializer: SequentialFileSerializer<T>
)