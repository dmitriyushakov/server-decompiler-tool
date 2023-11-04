package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.utils.data.*

class PersistedMethodSubject : MethodSubject {
    private val file: SequentialFile?
    private var pointer: EntityPointer<PersistedMethodSubject>? = null
    override val static: Boolean
    override val name: String
    override val descriptor: String
    override val path: List<String>
    override val sourcePath: String

    private var completeLocalVariableSubject: List<PersistedLocalVariableSubject>?
    private var completeDependencies: List<PersistedDependency>?
    private var localVariableSubjectPointers: List<EntityPointer<PersistedLocalVariableSubject>>?
    private var dependenciesPointers: List<EntityPointer<PersistedDependency>>?

    constructor(
        static: Boolean,
        name: String,
        descriptor: String,
        localVariableSubject: List<PersistedLocalVariableSubject>,
        path: List<String>,
        dependencies: List<PersistedDependency>,
        sourcePath: String
    ) {
        this.static = static
        this.name = name
        this.descriptor = descriptor
        this.path = path
        this.sourcePath = sourcePath

        file = null
        completeLocalVariableSubject = localVariableSubject
        completeDependencies = dependencies
        localVariableSubjectPointers = null
        dependenciesPointers = null
    }

    constructor(
        file: SequentialFile,
        static: Boolean,
        name: String,
        descriptor: String,
        localVariableSubject: List<EntityPointer<PersistedLocalVariableSubject>>,
        path: List<String>,
        dependencies: List<EntityPointer<PersistedDependency>>,
        sourcePath: String
    ) {
        this.static = static
        this.name = name
        this.descriptor = descriptor
        this.path = path
        this.sourcePath = sourcePath

        this.file = file
        completeLocalVariableSubject = null
        completeDependencies = null
        localVariableSubjectPointers = localVariableSubject
        dependenciesPointers = dependencies
    }

    override val localVariableSubject: List<PersistedLocalVariableSubject> by Delegates.localVariableSubjectLoadDelegate
    override val dependencies: List<PersistedDependency> by Delegates.dependenciesLoadDelegate

    override val owner: ClassSubject get() {
        throw UnsupportedOperationException("Access to method owner property is not implemented!")
    }
    object Serializer: SequentialFileSerializer<PersistedMethodSubject> {
        override fun toBytes(file: SequentialFile, offsetGetter: () -> Long, entity: PersistedMethodSubject): SequentialFileSerializer.Result<PersistedMethodSubject> {
            val pointer = entity.pointer
            if (pointer != null) return pointer.toResult()

            val bytes = dataBytes { data ->
                entity.apply {
                    data.writeBoolean(static)
                    data.writeString(name)
                    data.writeString(descriptor)
                    localVariableSubject.map { file.put(it, PersistedLocalVariableSubject.Serializer) }.let { data.writeEntityPointersList(it) }
                    data.writeStringsList(path)
                    dependencies.map { file.put(it, PersistedDependency.Serializer) }.let { data.writeEntityPointersList(it) }
                    data.writeString(sourcePath)
                }
            }

            entity.pointer = EntityPointer(offsetGetter(), bytes.size, this)
            return bytes.toResult()
        }

        override fun fromBytes(file: SequentialFile, offset: Long, bytes: ByteArray): PersistedMethodSubject {
            val data = bytes.getDataInputStream()
            val static = data.readBoolean()
            val name = data.readString()
            val descriptor = data.readString()
            val localVariableSubject = data.readEntityPointersList(PersistedLocalVariableSubject.Serializer)
            val path = data.readStringList()
            val dependencies = data.readEntityPointersList(PersistedDependency.Serializer)
            val sourcePath = data.readString()

            val entity = PersistedMethodSubject(
                file = file,
                static = static,
                name = name,
                descriptor = descriptor,
                localVariableSubject = localVariableSubject,
                path = path,
                dependencies = dependencies,
                sourcePath = sourcePath
            )

            entity.pointer = EntityPointer(offset, bytes.size, this)
            return entity
        }

    }

    private object Delegates {
        val localVariableSubjectLoadDelegate = EntitiesListFileLoadDelegate(PersistedMethodSubject::file, PersistedMethodSubject::completeLocalVariableSubject, PersistedMethodSubject::localVariableSubjectPointers)
        val dependenciesLoadDelegate = EntitiesListFileLoadDelegate(PersistedMethodSubject::file, PersistedMethodSubject::completeDependencies, PersistedMethodSubject::dependenciesPointers)
    }
}