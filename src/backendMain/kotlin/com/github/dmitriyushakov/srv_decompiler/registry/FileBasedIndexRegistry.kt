package com.github.dmitriyushakov.srv_decompiler.registry

import com.fasterxml.jackson.annotation.JsonAutoDetect.Value
import com.github.dmitriyushakov.srv_decompiler.common.blockfile.BlockFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.treefile.BlockFileTree
import com.github.dmitriyushakov.srv_decompiler.common.treefile.TreeFile
import com.github.dmitriyushakov.srv_decompiler.indexer.model.*
import com.github.dmitriyushakov.srv_decompiler.indexer.persisted.*
import com.github.dmitriyushakov.srv_decompiler.utils.data.dataBytes
import com.github.dmitriyushakov.srv_decompiler.utils.data.getDataInputStream
import com.github.dmitriyushakov.srv_decompiler.utils.data.readEntityPointersList
import com.github.dmitriyushakov.srv_decompiler.utils.data.writeEntityPointersList
import java.io.File
import kotlin.reflect.KMutableProperty1

private const val SUBJECTS_EXIST_FLAG = 0b001
private const val OUTGOING_DEPENDENCIES_EXIST_FLAG = 0b010
private const val INCOMING_DEPENDENCIES_EXIST_FLAG = 0b100

class FileBasedIndexRegistry(
    private val tree: TreeFile,
    private val entitiesFile: SequentialFile
): IndexRegistry, AutoCloseable {
    constructor(blockTreeFile: BlockFile, entitiesFile: SequentialFile): this(BlockFileTree(blockTreeFile), entitiesFile)
    constructor(treeFile: File, entitiesFile: File, isTemp: Boolean = true): this(BlockFile(treeFile, isTemp), SequentialFile(entitiesFile, isTemp))
    constructor(treeFilename: String, entitiesFilename: String, isTemp: Boolean = true): this(File(treeFilename), File(entitiesFilename), isTemp)

    private inner class Node (
        private val backNode: TreeFile.Node,
        classSubjects: List<EntityPointer<PersistedClassSubject>>,
        methodSubjects: List<EntityPointer<PersistedMethodSubject>>,
        fieldSubjects: List<EntityPointer<PersistedFieldSubject>>,
        localVariableSubjects: List<EntityPointer<PersistedLocalVariableSubject>>,
        subjectsExists: Boolean,
        outgoingDependencies: List<EntityPointer<PersistedDependency>>,
        outgoingDependenciesExists: Boolean,
        incomingDependencies: List<EntityPointer<PersistedDependency>>,
        incomingDependenciesExists: Boolean
    ) {

        val children: Map<String, Node> get() {
            val resultMap: MutableMap<String, Node> = mutableMapOf()

            for (keyBytes in backNode.keys) {
                val key = String(keyBytes)
                val node = backNode.get(keyBytes)!!.parseNode()
                resultMap[key] = node
            }

            return  resultMap
        }

        operator fun get(key: String): Node? {
            val keyBytes = key.toByteArray()
            return backNode[keyBytes]?.parseNode()
        }

        fun getOrCreate(key: String): Node {
            val keyBytes = key.toByteArray()
            return backNode.getOrCreate(keyBytes).parseNode()
        }

        var classSubjects: List<EntityPointer<PersistedClassSubject>> = classSubjects
            set(value) {
                field = value
                updatePayload()
            }
        var methodSubjects: List<EntityPointer<PersistedMethodSubject>> = methodSubjects
            set(value) {
                field = value
                updatePayload()
            }
        var fieldSubjects: List<EntityPointer<PersistedFieldSubject>> = fieldSubjects
            set(value) {
                field = value
                updatePayload()
            }
        var localVariableSubjects: List<EntityPointer<PersistedLocalVariableSubject>> = localVariableSubjects
            set(value) {
                field = value
                updatePayload()
            }
        var subjectsExists: Boolean = subjectsExists
            set(value) {
                field = value
                updatePayload()
            }
        var outgoingDependencies: List<EntityPointer<PersistedDependency>> = outgoingDependencies
            set(value) {
                field = value
                updatePayload()
            }
        var outgoingDependenciesExists: Boolean = outgoingDependenciesExists
            set(value) {
                field = value
                updatePayload()
            }
        var incomingDependencies: List<EntityPointer<PersistedDependency>> = incomingDependencies
            set(value) {
                field = value
                updatePayload()
            }
        var incomingDependenciesExists: Boolean = incomingDependenciesExists
            set(value) {
                field = value
                updatePayload()
            }

        private infix fun Boolean.and(flag: Int) = if (this) flag else 0

        private fun updatePayload() {
            backNode.payload = dataBytes { data ->
                data.writeEntityPointersList(classSubjects)
                data.writeEntityPointersList(methodSubjects)
                data.writeEntityPointersList(fieldSubjects)
                data.writeEntityPointersList(localVariableSubjects)
                val flags = (
                    (subjectsExists and SUBJECTS_EXIST_FLAG) or
                    (outgoingDependenciesExists and OUTGOING_DEPENDENCIES_EXIST_FLAG) or
                    (incomingDependenciesExists and INCOMING_DEPENDENCIES_EXIST_FLAG))
                data.writeByte(flags)
                data.writeEntityPointersList(outgoingDependencies)
                data.writeEntityPointersList(incomingDependencies)
            }
        }
    }

    private fun TreeFile.Node.parseNode(): Node {
        val data = payload.getDataInputStream()

        val classSubjects = data.readEntityPointersList(PersistedClassSubject.Serializer)
        val methodSubjects = data.readEntityPointersList(PersistedMethodSubject.Serializer)
        val fieldSubjects = data.readEntityPointersList(PersistedFieldSubject.Serializer)
        val localVariableSubjects = data.readEntityPointersList(PersistedLocalVariableSubject.Serializer)
        val flags = data.readUnsignedByte()
        val subjectsExists = (flags and SUBJECTS_EXIST_FLAG) != 0
        val outgoingDependencies = data.readEntityPointersList(PersistedDependency.Serializer)
        val outgoingDependenciesExists = (flags and OUTGOING_DEPENDENCIES_EXIST_FLAG) != 0
        val incomingDependencies = data.readEntityPointersList(PersistedDependency.Serializer)
        val incomingDependenciesExists = (flags and INCOMING_DEPENDENCIES_EXIST_FLAG) != 0

        return Node (
            backNode = this,
            classSubjects = classSubjects,
            methodSubjects = methodSubjects,
            fieldSubjects = fieldSubjects,
            localVariableSubjects = localVariableSubjects,
            subjectsExists = subjectsExists,
            outgoingDependencies = outgoingDependencies,
            outgoingDependenciesExists = outgoingDependenciesExists,
            incomingDependencies = incomingDependencies,
            incomingDependenciesExists = incomingDependenciesExists
        )
    }

    private val root: Node get() = tree.root.parseNode()

    private abstract inner class AbstractPathIndex<V>: PathIndex<V> {
        protected abstract var Node.valueExistence: Boolean
        protected abstract fun Node.addValue(value: V)
        protected abstract fun Node.getValues(): Collection<V>

        override fun add(path: Path, value: V) {
            var node = root
            node.valueExistence = true

            for (elem in path) {
                node = node.getOrCreate(elem)
                node.valueExistence = true
            }

            node.addValue(value)
        }

        override fun get(path: Path): Collection<V> {
            var node = root

            for (elem in path) {
                node = node[elem] ?: return emptyList()
                if (!node.valueExistence) return emptyList()
            }

            return node.getValues()
        }

        override fun contains(path: Path): Boolean {
            return this[path].isNotEmpty()
        }

        private fun <T> Collection<T>.asList(): List<T> = if (this is List<T>) this else toList()

        override fun getChildItems(path: Path): List<Pair<String, List<V>>> {
            var node = root

            for (elem in path) {
                node = node[elem] ?: return emptyList()
                if (!node.valueExistence) return emptyList()
            }

            return node
                .children
                .filter { it.value.valueExistence }
                .map { it.key to it.value.getValues().asList() }
                .sortedBy { it.first }
        }

        private fun visitForNextNode(path: Path, onlyRoot: Boolean, node: Node, results: MutableList<V>) {
            if (!node.valueExistence) return
            if (path.isEmpty()) {
                results.addAll(node.getValues())
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
            visitForNextNode(path, onlyRoot, root, results)
            return results
        }

        override fun findTopElement(path: Path, predicate: (Path, V) -> Boolean): Pair<Path, V>? {
            val currentPath = mutableListOf<String>()
            var node = root
            if (!node.valueExistence) return null

            for (res in node.getValues()) {
                if (predicate(emptyList(), res)) return emptyList<String>() to res
            }

            for (elem in path) {
                currentPath.add(elem)
                node = node[elem] ?: return null
                if (!node.valueExistence) return null

                val immutableCurrentPath: Path = currentPath.toList()
                for (res in node.getValues()) {
                    if (predicate(immutableCurrentPath, res)) return immutableCurrentPath to res
                }
            }

            return null
        }
    }

    private fun <T> List<T>.appended(value: T): List<T> = toMutableList().apply { add(value) }

    override val subjectsIndex: PathIndex<Subject> = object: AbstractPathIndex<Subject>() {
        override var Node.valueExistence: Boolean by Node::subjectsExists

        override fun Node.getValues() =
            listOf(classSubjects, methodSubjects, fieldSubjects, localVariableSubjects)
                .flatten().map { entitiesFile.get(it) }

        override fun Node.addValue(value: Subject) {
            when (value) {
                is ClassSubject -> {
                    val pointer = entitiesFile.put(value.toPersisted(), PersistedClassSubject.Serializer)
                    classSubjects = classSubjects.appended(pointer)
                }
                is MethodSubject -> {
                    val pointer = entitiesFile.put(value.toPersisted(), PersistedMethodSubject.Serializer)
                    methodSubjects = methodSubjects.appended(pointer)
                }
                is FieldSubject -> {
                    val pointer = entitiesFile.put(value.toPersisted(), PersistedFieldSubject.Serializer)
                    fieldSubjects = fieldSubjects.appended(pointer)
                }
                is LocalVariableSubject -> {
                    val pointer = entitiesFile.put(value.toPersisted(), PersistedLocalVariableSubject.Serializer)
                    localVariableSubjects = localVariableSubjects.appended(pointer)
                }
                else -> error("Unknown kind of subject")
            }
        }
    }
    override val outgoingDependenciesIndex: PathIndex<Dependency> = object: AbstractPathIndex<Dependency>() {
        override var Node.valueExistence: Boolean by Node::outgoingDependenciesExists
        override fun Node.getValues() = outgoingDependencies.map { entitiesFile.get(it) }
        override fun Node.addValue(value: Dependency) {
            val pointer = entitiesFile.put(value.toPersisted(), PersistedDependency.Serializer)
            outgoingDependencies = outgoingDependencies.appended(pointer)
        }
    }

    override val incomingDependenciesIndex: PathIndex<Dependency> = object: AbstractPathIndex<Dependency>() {
        override var Node.valueExistence: Boolean by Node::incomingDependenciesExists
        override fun Node.getValues() = incomingDependencies.map { entitiesFile.get(it) }
        override fun Node.addValue(value: Dependency) {
            val pointer = entitiesFile.put(value.toPersisted(), PersistedDependency.Serializer)
            incomingDependencies = incomingDependencies.appended(pointer)
        }
    }

    override fun close() {
        tree.close()
        entitiesFile.close()
    }
}