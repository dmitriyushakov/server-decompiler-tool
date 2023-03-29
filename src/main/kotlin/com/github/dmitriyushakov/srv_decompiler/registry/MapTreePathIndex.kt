package com.github.dmitriyushakov.srv_decompiler.registry

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MapTreePathIndex<T>: PathIndex<T> {
    private class TreeNode<T> {
        private val lock: Lock = ReentrantLock()
        private var childrenMapNullable: MutableMap<String, TreeNode<T>>? = null
        private var valuesMutNullable: MutableList<T>? = null

        // I expect huge tree of packages, classes, fields, methods and locals
        // so it will be better to tune earlier.
        private val childrenMap: MutableMap<String, TreeNode<T>>
            get() {
                var map = childrenMapNullable
                if (map == null) {
                    map = mutableMapOf()
                    childrenMapNullable = map
                }
                return map
            }

        private val valuesMut: MutableList<T>
            get() {
                var vals = valuesMutNullable
                if (vals == null) {
                    vals = mutableListOf()
                    valuesMutNullable = vals
                }
                return vals
            }

        val children: Map<String, TreeNode<T>> get() = childrenMapNullable?.let { childrenMap ->
            if (childrenMap.isEmpty()) emptyMap() else childrenMap.toMap()
        } ?: emptyMap()

        val values: List<T> get() = valuesMutNullable?.let { valuesMut ->
            if (valuesMut.isEmpty()) emptyList() else valuesMut.toList()
        } ?: emptyList()

        operator fun get(key: String): TreeNode<T>? = childrenMapNullable?.get(key)

        fun getOrCreate(key: String): TreeNode<T> {
            return lock.withLock {
                val node = childrenMap[key]
                if (node != null) {
                    node
                } else {
                    val newNode = TreeNode<T>()
                    childrenMap[key] = newNode
                    newNode
                }
            }
        }

        fun add(value: T) {
            lock.withLock {
                valuesMut.add(value)
            }
        }
    }

    private val rootNode: TreeNode<T> = TreeNode()

    override fun add(path: Path, value: T) {
        var node = rootNode
        for (elem in path) {
            node = node.getOrCreate(elem)
        }

        node.add(value)
    }

    override fun get(path: Path): Collection<T> {
        var node = rootNode

        for (elem in path) {
            node = node[elem] ?: return emptyList()
        }

        return node.values
    }

    override fun contains(path: Path): Boolean {
        return this[path].isNotEmpty()
    }

    override fun getChildItems(path: Path): List<Pair<String, List<T>>> {
        var node = rootNode

        for (elem in path) {
            node = node[elem] ?: return emptyList()
        }

        return node.children.map { it.key to it.value.values }.sortedBy { it.first }
    }

    private fun visitForNextNode(path: Path, onlyRoot: Boolean, node: TreeNode<T>, results: MutableList<T>) {
        if (path.isEmpty()) {
            results.addAll(node.values)
            return
        }

        val children = node.children
        val pathHead = path.first()
        val pathTail = if (path.size > 1) path.subList(1, path.size) else emptyList()

        if (children.containsKey(pathHead)) {
            val childNode = children[pathHead] ?: error("Tried access by existing key but got null")
            visitForNextNode(pathTail, true, childNode, results)
        }
        if(!onlyRoot) {
            for (childNode in children.values) {
                visitForNextNode(path, false, childNode, results)
            }
        }
    }

    override fun searchForPath(path: Path, onlyRoot: Boolean): List<T> {
        val results: MutableList<T> = mutableListOf()
        visitForNextNode(path, onlyRoot, rootNode, results)
        return results
    }
}