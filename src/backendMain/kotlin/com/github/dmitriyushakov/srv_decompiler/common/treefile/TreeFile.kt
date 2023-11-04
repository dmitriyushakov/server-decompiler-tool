package com.github.dmitriyushakov.srv_decompiler.common.treefile

interface TreeFile: AutoCloseable {
    interface Node {
        val keys: List<ByteArray>
        var payload: ByteArray

        operator fun get(key: ByteArray): Node?
        fun getOrCreate(key: ByteArray): Node
    }

    val root: Node

    fun flush()
    fun commit()
    fun reject()
}