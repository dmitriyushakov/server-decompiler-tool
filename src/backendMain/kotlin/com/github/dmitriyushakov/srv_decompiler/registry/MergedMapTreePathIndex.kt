package com.github.dmitriyushakov.srv_decompiler.registry

import kotlin.reflect.KMutableProperty1

private val INITIAL_CHILDREN_CAPACITY = 4

abstract class MergedMapTreePathIndex<T: MergedMapTreePathIndex.AbstractTreeNode<T>> {
    abstract val nodeCreator: () -> T
    protected val rootNode: T by lazy { nodeCreator() }
    abstract class AbstractTreeNode<T: AbstractTreeNode<T>>(private val creator:() -> T) {
        private var kvChildrenArray: Array<Any?> = arrayOfNulls(INITIAL_CHILDREN_CAPACITY * 2)

        @Suppress("UNCHECKED_CAST")
        val children: Map<String, T> get()  {
            val resultMap: MutableMap<String, T> = mutableMapOf()
            var i = 0
            val kvChildrenArray = this.kvChildrenArray
            while (i < kvChildrenArray.size) {
                val keyOrNull = kvChildrenArray[i] ?: break

                val key = (keyOrNull as? String) ?: error("String expected in odd index!")
                val value: T = (kvChildrenArray[i + 1] as? T) ?: error("Child node expected in even index!")

                resultMap[key] = value
                i += 2
            }

            return resultMap
        }

        @Suppress("UNCHECKED_CAST")
        operator fun get(key: String): T? {
            var i = 0
            val kvChildrenArray = this.kvChildrenArray
            while (i < kvChildrenArray.size) {
                val keyOrNull = kvChildrenArray[i] ?: break

                val currentKey = (keyOrNull as? String) ?: error("String expected in odd index!")
                if (key == currentKey) {
                    return (kvChildrenArray[i + 1] as? T) ?: error("Child node expected in even index!")
                } else {
                    i += 2
                }
            }

            return null
        }

        @Suppress("UNCHECKED_CAST")
        fun getOrCreate(key: String): T {
            synchronized(this) {
                var i = 0
                var kvChildrenArray = this.kvChildrenArray
                while (i < kvChildrenArray.size) {
                    val keyOrNull = kvChildrenArray[i] ?: break

                    val currentKey = (keyOrNull as? String) ?: error("String expected in odd index!")
                    if (key == currentKey) {
                        return (kvChildrenArray[i + 1] as? T) ?: error("Child node expected in even index!")
                    } else {
                        i += 2
                    }
                }

                if (i >= kvChildrenArray.size) {
                    val newKvChildrenArray: Array<Any?> = arrayOfNulls(kvChildrenArray.size * 2)
                    System.arraycopy(kvChildrenArray, 0, newKvChildrenArray, 0, kvChildrenArray.size)
                    kvChildrenArray = newKvChildrenArray
                    this.kvChildrenArray = newKvChildrenArray
                }

                val newNode = creator()
                kvChildrenArray[i + 1] = newNode
                kvChildrenArray[i] = key

                return newNode
            }
        }
    }

    class ParticularPathIndex<T: AbstractTreeNode<T>, V>(
        private val rootNode: T,
        private val valueListProperty: KMutableProperty1<T, MutableList<V>?>,
        private val valueExistenceFlagProperty: KMutableProperty1<T, Boolean>
    ): PathIndex<V> {

        private fun getValuesForUpdate(node: T): MutableList<V> {
            return synchronized(node) {
                val valuesList = valueListProperty.get(node)
                if (valuesList == null) {
                    val newValuesList = mutableListOf<V>()
                    valueListProperty.set(node, newValuesList)
                    newValuesList
                } else {
                    valuesList
                }
            }
        }

        private fun getValuesForRead(node: T): List<V> {
            return valueListProperty.get(node) ?: emptyList()
        }

        override fun add(path: Path, value: V) {
            var node = rootNode
            valueExistenceFlagProperty.set(node, true)

            for (elem in path) {
                node = node.getOrCreate(elem)
                valueExistenceFlagProperty.set(node, true)
            }

            getValuesForUpdate(node).add(value)
        }

        override fun get(path: Path): Collection<V> {
            var node = rootNode

            for (elem in path) {
                node = node[elem] ?: return emptyList()
                if (!valueExistenceFlagProperty.get(node)) return emptyList()
            }

            return getValuesForRead(node)
        }

        override fun contains(path: Path): Boolean {
            return this[path].isNotEmpty()
        }

        override fun getChildItems(path: Path): List<Pair<String, List<V>>> {
            var node = rootNode

            for (elem in path) {
                node = node[elem] ?: return emptyList()
                if (!valueExistenceFlagProperty.get(node)) return emptyList()
            }

            return node
                .children
                .filter { valueExistenceFlagProperty.get(it.value) }
                .map { it.key to getValuesForRead(it.value) }
                .sortedBy { it.first }
        }

        private fun visitForNextNode(path: Path, onlyRoot: Boolean, node: T, results: MutableList<V>) {
            if (!valueExistenceFlagProperty(node)) return
            if (path.isEmpty()) {
                results.addAll(getValuesForRead(node))
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

        override fun searchForPath(path: Path, onlyRoot: Boolean): List<V> {
            val results: MutableList<V> = mutableListOf()
            visitForNextNode(path, onlyRoot, rootNode, results)
            return results
        }

        override fun findTopElement(path: Path, predicate: (Path, V) -> Boolean): Pair<Path, V>? {
            val currentPath = mutableListOf<String>()
            var node = rootNode
            if (!valueExistenceFlagProperty.get(node)) return null

            for (res in getValuesForRead(node)) {
                if (predicate(emptyList(), res)) return emptyList<String>() to res
            }

            for (elem in path) {
                currentPath.add(elem)
                node = node[elem] ?: return null
                if (!valueExistenceFlagProperty.get(node)) return null

                val immutableCurrentPath: Path = currentPath.toList()
                for (res in getValuesForRead(node)) {
                    if (predicate(immutableCurrentPath, res)) return immutableCurrentPath to res
                }
            }

            return null
        }

    }
}