package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.indexer.model.LocalVariableSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.utils.data.*

class PersistedLocalVariableSubject : LocalVariableSubject {
    private val file: SequentialFile?
    private var pointer: EntityPointer<PersistedLocalVariableSubject>? = null
    override val name: String
    override val descriptor: String
    override val path: List<String>
    override val sourcePath: String


    private var completeDependencies: List<PersistedDependency>?
    private var dependencyPointers: List<EntityPointer<PersistedDependency>>?

    constructor(
        name: String,
        descriptor: String,
        path: List<String>,
        dependencies: List<PersistedDependency>,
        sourcePath: String
    ) {
        this.name = name
        this.descriptor = descriptor
        this.path = path
        this.sourcePath = sourcePath

        file = null
        completeDependencies = dependencies
        dependencyPointers = null
    }

    constructor(
        file: SequentialFile,
        name: String,
        descriptor: String,
        path: List<String>,
        dependencies: List<EntityPointer<PersistedDependency>>,
        sourcePath: String
    ) {
        this.name = name
        this.descriptor = descriptor
        this.path = path
        this.sourcePath = sourcePath

        this.file = file
        completeDependencies = null
        dependencyPointers = dependencies
    }

    override val owner: MethodSubject get() {
        throw UnsupportedOperationException("Not implemented access to persisted local var owner!")
    }
    override val childrenSubjects: List<Subject> get() = emptyList()
    override val dependencies: List<PersistedDependency> by Delegates.dependenciesLoadDelegate

    object Serializer: SequentialFileSerializer<PersistedLocalVariableSubject> {
        override fun toBytes(file: SequentialFile, offsetGetter: () -> Long, entity: PersistedLocalVariableSubject): SequentialFileSerializer.Result<PersistedLocalVariableSubject> {
            val pointer = entity.pointer
            if (pointer != null) return pointer.toResult()

            val bytes = dataBytes { data ->
                entity.apply {
                    data.writeString(name)
                    data.writeString(descriptor)
                    data.writeStringsList(path)
                    dependencies.map { file.put(it, PersistedDependency.Serializer) }.let { data.writeEntityPointersList(it) }
                    data.writeString(sourcePath)
                }
            }

            entity.pointer = EntityPointer(offsetGetter(), bytes.size, this)
            return bytes.toResult()
        }

        override fun fromBytes(file: SequentialFile, offset: Long, bytes: ByteArray): PersistedLocalVariableSubject {
            val data = bytes.getDataInputStream()

            val name = data.readString()
            val descriptor = data.readString()
            val path = data.readStringList()
            val dependencies = data.readEntityPointersList(PersistedDependency.Serializer)
            val sourcePath = data.readString()

            val entity = PersistedLocalVariableSubject(
                file = file,
                name = name,
                descriptor = descriptor,
                path = path,
                dependencies = dependencies,
                sourcePath = sourcePath
            )
            entity.pointer = EntityPointer(offset, bytes.size, this)

            return entity
        }
    }

    private object Delegates {
        val dependenciesLoadDelegate = EntitiesListFileLoadDelegate(PersistedLocalVariableSubject::file, PersistedLocalVariableSubject::completeDependencies, PersistedLocalVariableSubject::dependencyPointers)
    }
}