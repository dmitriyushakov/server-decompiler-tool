package com.github.dmitriyushakov.srv_decompiler.common.treefile

class LoadedPayload(bytes: ByteArray): LazyPayload {
    private val bytes = bytes.clone()
    override fun toByteArray() = bytes.clone()
    override fun hashCode() = bytes.contentHashCode()
    override fun equals(other: Any?): Boolean {
        if (other !is LoadedPayload) return false
        return bytes contentEquals other.bytes
    }
}