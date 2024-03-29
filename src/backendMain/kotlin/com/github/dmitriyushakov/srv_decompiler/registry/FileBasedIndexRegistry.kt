package com.github.dmitriyushakov.srv_decompiler.registry

import com.github.dmitriyushakov.srv_decompiler.common.blockfile.BlockFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.treefile.BlockFileTree
import com.github.dmitriyushakov.srv_decompiler.common.treefile.LazyPayload
import com.github.dmitriyushakov.srv_decompiler.common.treefile.TreeFile
import com.github.dmitriyushakov.srv_decompiler.indexer.model.*
import com.github.dmitriyushakov.srv_decompiler.indexer.persisted.*
import com.github.dmitriyushakov.srv_decompiler.utils.data.dataBytes
import com.github.dmitriyushakov.srv_decompiler.utils.data.getDataInputStream
import com.github.dmitriyushakov.srv_decompiler.utils.data.readEntityPointersList
import com.github.dmitriyushakov.srv_decompiler.utils.data.writeEntityPointersList
import java.io.File
import java.util.*
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

private const val SUBJECTS_EXIST_FLAG = 0b001
private const val OUTGOING_DEPENDENCIES_EXIST_FLAG = 0b010
private const val INCOMING_DEPENDENCIES_EXIST_FLAG = 0b100

class FileBasedIndexRegistry(
    private val tree: TreeFile,
    private val entitiesFile: SequentialFile
): IndexRegistry, AutoCloseable {
    var autocommit: Boolean = false
    constructor(blockTreeFile: BlockFile, entitiesFile: SequentialFile): this(BlockFileTree(blockTreeFile), entitiesFile)
    constructor(treeFile: File, entitiesFile: File, isTemp: Boolean = true, compress: Boolean = false): this(BlockFile(treeFile, isTemp), SequentialFile(entitiesFile, isTemp, compress))
    constructor(treeFilename: String, entitiesFilename: String, isTemp: Boolean = true, compress: Boolean = false): this(File(treeFilename), File(entitiesFilename), isTemp, compress)

    private inner class ImmutableParsedNodeData (
        val classSubjects: List<EntityPointer<PersistedClassSubject>>,
        val methodSubjects: List<EntityPointer<PersistedMethodSubject>>,
        val fieldSubjects: List<EntityPointer<PersistedFieldSubject>>,
        val localVariableSubjects: List<EntityPointer<PersistedLocalVariableSubject>>,
        val subjectsExists: Boolean,
        val outgoingDependencies: List<EntityPointer<PersistedDependency>>,
        val outgoingDependenciesExists: Boolean,
        val incomingDependencies: List<EntityPointer<PersistedDependency>>,
        val incomingDependenciesExists: Boolean
    ) {
        fun bindToPhysicalNode(backNode: TreeFile.Node) = ParsedNodeData(
            backNode = backNode,
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

        private infix fun Boolean.and(flag: Int) = if (this) flag else 0

        fun getBytesPayload(): ByteArray {
            return dataBytes { data ->
                val flags = (
                        (subjectsExists and SUBJECTS_EXIST_FLAG) or
                        (outgoingDependenciesExists and OUTGOING_DEPENDENCIES_EXIST_FLAG) or
                        (incomingDependenciesExists and INCOMING_DEPENDENCIES_EXIST_FLAG))
                data.writeByte(flags)
                data.writeEntityPointersList(classSubjects)
                data.writeEntityPointersList(methodSubjects)
                data.writeEntityPointersList(fieldSubjects)
                data.writeEntityPointersList(localVariableSubjects)
                data.writeEntityPointersList(outgoingDependencies)
                data.writeEntityPointersList(incomingDependencies)
            }
        }

        override fun hashCode(): Int {
            return Objects.hash(classSubjects, methodSubjects, fieldSubjects, localVariableSubjects, subjectsExists,
                outgoingDependencies, outgoingDependenciesExists, incomingDependencies, incomingDependenciesExists)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is ImmutableParsedNodeData) return false
            return classSubjects == other.classSubjects && methodSubjects == other.methodSubjects &&
                    fieldSubjects == other.fieldSubjects && localVariableSubjects == other.localVariableSubjects &&
                    subjectsExists == other.subjectsExists && outgoingDependencies == other.outgoingDependencies &&
                    outgoingDependenciesExists == other.outgoingDependenciesExists &&
                    incomingDependencies == other.incomingDependencies &&
                    incomingDependenciesExists == other.incomingDependenciesExists
        }
    }

    private class ImmutableParsedNodeDataPayload(val data: FileBasedIndexRegistry.ImmutableParsedNodeData): LazyPayload {
        private var cachedPayload: ByteArray? = null
        override fun toByteArray(): ByteArray {
            val cachedPayload = this.cachedPayload
            if (cachedPayload == null) {
                val newPayload = data.getBytesPayload()
                this.cachedPayload = newPayload
                return newPayload.clone()
            } else {
                return cachedPayload.clone()
            }
        }

        override fun hashCode() = data.hashCode()
        override fun equals(other: Any?): Boolean {
            if (other !is ImmutableParsedNodeDataPayload) return false
            return data == other.data
        }
    }

    private inner class ParsedNodeData (
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
                val updated = value != field
                field = value
                if (updated) updatePayload()
            }
        var outgoingDependencies: List<EntityPointer<PersistedDependency>> = outgoingDependencies
            set(value) {
                field = value
                updatePayload()
            }
        var outgoingDependenciesExists: Boolean = outgoingDependenciesExists
            set(value) {
                val updated = value != field
                field = value
                if (updated) updatePayload()
            }
        var incomingDependencies: List<EntityPointer<PersistedDependency>> = incomingDependencies
            set(value) {
                field = value
                updatePayload()
            }
        var incomingDependenciesExists: Boolean = incomingDependenciesExists
            set(value) {
                val updated = value != field
                field = value
                if (updated) updatePayload()
            }

        private fun updatePayload() {
            backNode.payload = toImmutable().let(::ImmutableParsedNodeDataPayload)
        }

        fun toImmutable(): ImmutableParsedNodeData = ImmutableParsedNodeData(
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

    private inner class LazyParsedNodeDataDelegate<V>(
        private val backNodeProperty: KProperty0<TreeFile.Node>,
        private val parsedNodeProperty: KMutableProperty0<ParsedNodeData?>,
        private val dataProperty: KMutableProperty1<ParsedNodeData, V>) {
        private fun getParsedData(): ParsedNodeData {
            val parsedNode = parsedNodeProperty.get()
            if (parsedNode == null) {
                val backNode = backNodeProperty.get()
                val newParsedNode = parseNode(backNode.payload).bindToPhysicalNode(backNode)
                parsedNodeProperty.set(newParsedNode)
                return newParsedNode
            } else {
                return parsedNode
            }
        }
        operator fun getValue(thisRef: Any?, property: KProperty<*>): V{
            return dataProperty.get(getParsedData())
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            dataProperty.set(getParsedData(), value)
        }
    }

    private inner class Node (
        backNodeSupplier: () -> TreeFile.Node
    ) {
        private val backNode by lazy(backNodeSupplier)
        private var parsedNodeData: ParsedNodeData? = null
        private fun <V> lazyData(dataProperty: KMutableProperty1<ParsedNodeData, V>) =
            LazyParsedNodeDataDelegate(this::backNode, this::parsedNodeData, dataProperty)

        val childrenMut: MutableMap<String, Node> by lazy { calculateChildren() }
        val children: Map<String, Node> get() = childrenMut.toMap()

        private fun calculateChildren(): MutableMap<String, Node> {
            val resultMap: MutableMap<String, Node> = mutableMapOf()

            for (keyBytes in backNode.keys) {
                val key = String(keyBytes)
                val node = Node { backNode[keyBytes]!! }
                resultMap[key] = node
            }

            return resultMap
        }

        operator fun get(key: String): Node? {
            return childrenMut[key]
        }

        fun getOrCreate(key: String): Node {
            return childrenMut.computeIfAbsent(key) { mapKey ->
                Node {
                    backNode.getOrCreate(mapKey.toByteArray())
                }
            }
        }

        var classSubjects: List<EntityPointer<PersistedClassSubject>> by lazyData(ParsedNodeData::classSubjects)
        var methodSubjects: List<EntityPointer<PersistedMethodSubject>> by lazyData(ParsedNodeData::methodSubjects)
        var fieldSubjects: List<EntityPointer<PersistedFieldSubject>> by lazyData(ParsedNodeData::fieldSubjects)
        var localVariableSubjects: List<EntityPointer<PersistedLocalVariableSubject>> by lazyData(ParsedNodeData::localVariableSubjects)
        var subjectsExists: Boolean by lazyData(ParsedNodeData::subjectsExists)
        var outgoingDependencies: List<EntityPointer<PersistedDependency>> by lazyData(ParsedNodeData::outgoingDependencies)
        var outgoingDependenciesExists: Boolean by lazyData(ParsedNodeData::outgoingDependenciesExists)
        var incomingDependencies: List<EntityPointer<PersistedDependency>> by lazyData(ParsedNodeData::incomingDependencies)
        var incomingDependenciesExists: Boolean by lazyData(ParsedNodeData::incomingDependenciesExists)
    }

    private fun parseNode(lazyPayload: LazyPayload): ImmutableParsedNodeData {
        if (lazyPayload is ImmutableParsedNodeDataPayload) return lazyPayload.data

        val payload = lazyPayload()

        if (payload.isEmpty()) {
            return ImmutableParsedNodeData (
                classSubjects = emptyList(),
                methodSubjects = emptyList(),
                fieldSubjects = emptyList(),
                localVariableSubjects = emptyList(),
                subjectsExists = false,
                outgoingDependencies = emptyList(),
                outgoingDependenciesExists = false,
                incomingDependencies = emptyList(),
                incomingDependenciesExists = false
            )
        }

        val data = payload.getDataInputStream()

        val flags = data.readUnsignedByte()
        val classSubjects = data.readEntityPointersList(PersistedClassSubject.Serializer)
        val methodSubjects = data.readEntityPointersList(PersistedMethodSubject.Serializer)
        val fieldSubjects = data.readEntityPointersList(PersistedFieldSubject.Serializer)
        val localVariableSubjects = data.readEntityPointersList(PersistedLocalVariableSubject.Serializer)
        val subjectsExists = (flags and SUBJECTS_EXIST_FLAG) != 0
        val outgoingDependencies = data.readEntityPointersList(PersistedDependency.Serializer)
        val outgoingDependenciesExists = (flags and OUTGOING_DEPENDENCIES_EXIST_FLAG) != 0
        val incomingDependencies = data.readEntityPointersList(PersistedDependency.Serializer)
        val incomingDependenciesExists = (flags and INCOMING_DEPENDENCIES_EXIST_FLAG) != 0

        return ImmutableParsedNodeData (
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

    private var currentRoot: Node? = null
    private val root: Node get() {
        val currentRoot = this.currentRoot
        if (currentRoot != null) return currentRoot
        synchronized(this) {
            val secondAttemptNode = this.currentRoot
            if (secondAttemptNode != null) return secondAttemptNode

            val newRootNode = Node { tree.root }
            this.currentRoot = newRootNode
            return newRootNode
        }
    }

    private abstract inner class AbstractPathIndex<V>: PathIndex<V> {
        protected abstract var Node.valueExistence: Boolean
        protected abstract fun Node.addValue(value: V)
        protected abstract fun Node.getValues(): Collection<V>

        override fun add(path: Path, value: V) {
            try {
                var node = root
                node.valueExistence = true

                for (elem in path) {
                    node = node.getOrCreate(elem)
                    node.valueExistence = true
                }

                node.addValue(value)
            } finally {
                if (autocommit) commit()
            }
        }

        override fun get(path: Path): Collection<V> {
            try {
                var node = root

                for (elem in path) {
                    node = node[elem] ?: return emptyList()
                    if (!node.valueExistence) return emptyList()
                }

                return node.getValues()
            } finally {
                if (autocommit) commit()
            }
        }

        override fun contains(path: Path): Boolean {
            return this[path].isNotEmpty()
        }

        private fun <T> Collection<T>.asList(): List<T> = if (this is List<T>) this else toList()

        override fun getChildItems(path: Path): List<Pair<String, List<V>>> {
            try {
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
            } finally {
                if (autocommit) commit()
            }
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
            try {
                val results: MutableList<V> = mutableListOf()
                visitForNextNode(path, onlyRoot, root, results)
                return results
            } finally {
                if (autocommit) commit()
            }
        }

        override fun findTopElement(path: Path, predicate: (Path, V) -> Boolean): Pair<Path, V>? {
            try {
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
            } finally {
                if (autocommit) commit()
            }
        }
    }

    private fun <T> List<T>.appended(value: T): List<T> = toMutableList().apply { add(value) }

    override val subjectsIndex: PathIndex<Subject> = object: AbstractPathIndex<Subject>() {
        override var Node.valueExistence: Boolean by Node::subjectsExists

        override fun Node.getValues(): Collection<Subject> =
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

    override fun commit() {
        tree.commit()
        currentRoot = null
    }

    fun flush() {
        tree.flush()
    }

    override fun close() {
        tree.close()
        entitiesFile.close()
    }
}