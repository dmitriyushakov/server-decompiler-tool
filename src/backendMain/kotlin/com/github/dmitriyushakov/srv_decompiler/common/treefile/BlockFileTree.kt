package com.github.dmitriyushakov.srv_decompiler.common.treefile

import com.github.dmitriyushakov.srv_decompiler.common.blockfile.BlockFile
import com.github.dmitriyushakov.srv_decompiler.utils.data.dataBytes
import com.github.dmitriyushakov.srv_decompiler.utils.data.getDataInputStream
import java.io.File

private const val NODE_CACHE_SIZE: Int = 100

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
        var index: Int?,
        var modified: Boolean,
        kvPairs: List<Pair<ByteArray, Int>>,
        payload: LazyPayload
    ): TreeFile.Node {


        override var payload: LazyPayload = payload
            set(value) {
                synchronized(this) {
                    modified = modified || field != value
                    field = value
                }
            }

        var kvPairs: List<Pair<ByteArray, Int>> = kvPairs
            set(value) {
                modified = true
                field = value
            }

        private val newNodes: MutableList<Pair<ByteArray, BlockFileNode>> = mutableListOf()

        override val keys: List<ByteArray> get() = kvPairs.map { it.first }

        private fun synchronizedGet(key: ByteArray): TreeFile.Node? {
            synchronized(this) {
                val newNode = newNodes.firstOrNull { it.first contentEquals key }?.second
                if (newNode != null) return newNode

                val nodeIndex = kvPairs.firstOrNull { it.first contentEquals key }?.second ?: return null
                val loadedNode = getNode(nodeIndex)
                return loadedNode
            }
        }

        override operator fun get(key: ByteArray): TreeFile.Node? {
            return synchronizedGet(key)
        }

        override fun getOrCreate(key: ByteArray): TreeFile.Node {
            synchronized(this) {
                val node = synchronizedGet(key)
                if (node != null) return  node

                val newNode = BlockFileNode(null, true, emptyList(), LazyPayload.Empty)
                newNodes.add(key to newNode)
                modified = true
                return newNode
            }
        }

        fun commit(partial: Boolean = false) {
            synchronized(this) {
                val newKvPairs = kvPairs.toMutableList()
                var kvPairsUpdated = false

                for ((key, node) in newNodes) {
                    node.commit()
                    val nodeIdx = node.index ?: error("Not null index expected after node commit!")
                    newKvPairs.add(key to nodeIdx)
                    kvPairsUpdated = true
                }
                if (kvPairsUpdated) {
                    newNodes.clear()
                    kvPairs = newKvPairs
                }
                if (modified && (!partial || index == null)) {
                    storeNode(this)
                    modified = false
                    nodeCache[index!!] = this
                }
            }
        }
    }

    private fun getNode(index: Int): BlockFileNode {
        return nodeCache.computeIfAbsent(index, ::loadNode)
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

        val payload = data.readAllBytes().let(::LoadedPayload)
        return BlockFileNode(index, false, kvPairs, payload)
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

            data.write(node.payload())
        }

        val idx = node.index
        if (idx != null) {
            blockFile.put(idx, recordBytes)
        } else {
            node.index = blockFile.put(recordBytes)
        }
    }

    private val nodeCache = LinkedHashMap<Int, BlockFileNode>(NODE_CACHE_SIZE * 4 / 3, 0.75f, true)

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
                    val initialRoot = BlockFileNode(0, true, emptyList(), LazyPayload.Empty)
                    initialRoot.commit()
                    nodeCache[0] = initialRoot
                    loadedRoot = initialRoot
                    return initialRoot
                } else {
                    val loadedRoot = getNode(0)
                    this.loadedRoot = loadedRoot
                    return loadedRoot
                }
            }
        }

    override fun flush() {
        synchronized(this) {
            fullCommit()
            blockFile.flush()
        }
    }

    private fun fullCommit() {
        synchronized(this) {
            val nodesToCommit: List<BlockFileNode>
            if (nodeCache.size > NODE_CACHE_SIZE) {
                val trimNodesCount = nodeCache.size - NODE_CACHE_SIZE
                val indicesToRemove = nodeCache.keys.take(trimNodesCount)
                nodesToCommit = nodeCache.values.filter { it.modified }
                for (idx in indicesToRemove) nodeCache.remove(idx)
            } else {
                nodesToCommit = nodeCache.values.filter { it.modified }
            }
            for (node in nodesToCommit) node.commit()
            loadedRoot = null
        }
    }

    override fun commit() {
        synchronized(this) {
            val nodesToCommit: List<BlockFileNode>
            if (nodeCache.size > NODE_CACHE_SIZE) {
                val trimNodesCount = nodeCache.size - NODE_CACHE_SIZE
                val indicesToRemove = nodeCache.values.filter { !it.modified }.mapNotNull { it.index }.take(trimNodesCount)
                nodesToCommit = nodeCache.values.filter { it.modified }
                for (idx in indicesToRemove) nodeCache.remove(idx)
            } else {
                nodesToCommit = nodeCache.values.filter { it.modified }
            }
            for (node in nodesToCommit) node.commit(partial = nodesToCommit.size < NODE_CACHE_SIZE / 2)
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