package com.github.dmitriyushakov.srv_decompiler.common.treefile

import com.github.dmitriyushakov.srv_decompiler.common.blockfile.BlockFile
import com.github.dmitriyushakov.srv_decompiler.utils.data.dataBytes
import com.github.dmitriyushakov.srv_decompiler.utils.data.getDataInputStream
import java.io.File

class BlockFileTree(val blockFile: BlockFile): TreeFile, AutoCloseable {
    private class ByteArrayKey(val arr: ByteArray) {
        override fun hashCode(): Int = arr.sumOf { it.toInt() }
        override fun equals(other: Any?): Boolean {
            if (other !is ByteArrayKey) return false
            return arr contentEquals other.arr
        }
    }

    private fun ByteArray.asKey() = ByteArrayKey(this)

    private inner class BlockFileNode(
        var index: Int? = null,
        kvPairs: List<Pair<ByteArray, Int>>,
        payload: ByteArray
    ): TreeFile.Node {
        private var modified = index == null

        override var payload: ByteArray = payload
            set(value) {
                synchronized(this) {
                    modified = modified || !(field contentEquals value)
                    field = value
                }
            }

        var kvPairs: List<Pair<ByteArray, Int>> = kvPairs
            set(value) {
                modified = true
                field = value
            }

        private val loadedNodes: MutableList<Pair<ByteArray, BlockFileNode>> = mutableListOf()

        override val keys: List<ByteArray> get() = kvPairs.map { it.first }

        private fun synchronizedGet(key: ByteArray): TreeFile.Node? {
            synchronized(this) {
                val loadedNode = loadedNodes.firstOrNull { it.first contentEquals key }?.second
                if (loadedNode != null) return loadedNode

                val nodeIndex = kvPairs.firstOrNull { it.first contentEquals key }?.second ?: return null
                val newLoadedNode = loadNode(nodeIndex)
                loadedNodes.add(key to newLoadedNode)
                return newLoadedNode
            }
        }

        override operator fun get(key: ByteArray): TreeFile.Node? {
            val firstAttempt = loadedNodes.firstOrNull { it.first contentEquals key }?.second
            if (firstAttempt != null) return firstAttempt

            return synchronizedGet(key)
        }

        override fun getOrCreate(key: ByteArray): TreeFile.Node {
            val firstAttempt = loadedNodes.firstOrNull { it.first contentEquals key }?.second
            if (firstAttempt != null) return firstAttempt

            synchronized(this) {
                val node = synchronizedGet(key)
                if (node != null) return  node

                val newNode = BlockFileNode(null, emptyList(), ByteArray(0))
                loadedNodes.add(key to newNode)
                return newNode
            }
        }

        fun commit() {
            synchronized(this) {
                val oldKeys = kvPairs.map { it.first.asKey() }.toSet()
                val newKvPairs = kvPairs.toMutableList()
                var kvPairsUpdated = false

                for ((key, node) in loadedNodes) {
                    node.commit()
                    val nodeIdx = node.index ?: error("Not null index expected after node commit!")
                    if (key.asKey() !in oldKeys) {
                        newKvPairs.add(key to nodeIdx)
                        kvPairsUpdated = true
                    }
                }

                if (kvPairsUpdated) kvPairs = newKvPairs
                if (modified) {
                    storeNode(this)
                    modified = false
                }
            }
        }
    }

    private fun loadNode(index: Int): BlockFileNode {
        val recordBytes = blockFile.get(index)
        val data = recordBytes.getDataInputStream()
        val kvPairs: MutableList<Pair<ByteArray, Int>> = mutableListOf()

        val kvSize = data.readUnsignedShort()
        for (i in 0 until kvSize) {
            val keySize = data.readUnsignedShort()
            val key = ByteArray(keySize)
            data.read(key)
            val kvIndex = data.readInt()
            kvPairs.add(key to kvIndex)
        }

        val payload = data.readAllBytes()
        val node = BlockFileNode(index, kvPairs, payload)

        return node
    }

    private fun storeNode(node: BlockFileNode) {
        val recordBytes = dataBytes { data ->
            val pairs = node.kvPairs
            data.writeShort(pairs.size)
            for ((key, idx) in pairs) {
                data.writeShort(key.size)
                data.write(key)
                data.writeInt(idx)
            }

            data.write(node.payload)
        }

        val idx = node.index
        if (idx != null) {
            blockFile.put(idx, recordBytes)
        } else {
            node.index = blockFile.put(recordBytes)
        }
    }


    constructor(file: File, isTemp: Boolean = true): this(BlockFile(file, isTemp))
    constructor(filename: String, isTemp: Boolean = true): this(File(filename), isTemp)

    private var loadedRoot: BlockFileNode? = null

    override val root: TreeFile.Node
        get() {
            val firstAttempt = loadedRoot
            if (firstAttempt != null) return firstAttempt

            synchronized(this) {
                val root = loadedRoot
                if (root != null) return root

                if (blockFile.size == 0) {
                    val initialRoot = BlockFileNode(0, emptyList(), ByteArray(0))
                    initialRoot.commit()
                    loadedRoot = initialRoot
                    return initialRoot
                } else {
                    val loadedRoot = loadNode(0)
                    this.loadedRoot = loadedRoot
                    return loadedRoot
                }
            }
        }

    override fun flush() {
        synchronized(this) {
            blockFile.flush()
        }
    }

    override fun commit() {
        synchronized(this) {
            loadedRoot?.commit()
            loadedRoot = null
        }
    }

    override fun reject() {
        loadedRoot = null
    }

    override fun close() {
        blockFile.close()
    }
}