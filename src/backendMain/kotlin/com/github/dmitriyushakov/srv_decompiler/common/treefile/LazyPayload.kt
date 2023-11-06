package com.github.dmitriyushakov.srv_decompiler.common.treefile

interface LazyPayload: () -> ByteArray {
    fun toByteArray(): ByteArray
    override fun invoke() = toByteArray()

    object Empty: LazyPayload {
        private val emptyByteArray = ByteArray(0)
        override fun toByteArray() = emptyByteArray
        override fun hashCode() = 0
        override fun equals(other: Any?) = other == this
    }
}