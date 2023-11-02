package com.github.dmitriyushakov.srv_decompiler.common.seqfile

import java.util.*

class EntityPointer<T>(
    val offset: Long,
    val size: Int,
    val serializer: SequentialFileSerializer<T>
) {
    override fun hashCode(): Int = Objects.hash(offset, size)
    override fun equals(other: Any?): Boolean {
        if (other !is EntityPointer<*>) return false
        return other.offset == offset && other.size == size && other.serializer == serializer
    }
}