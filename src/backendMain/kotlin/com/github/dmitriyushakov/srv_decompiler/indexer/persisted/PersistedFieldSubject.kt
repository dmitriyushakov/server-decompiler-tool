package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializable
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.utils.data.*

class PersistedFieldSubject : FieldSubject, SequentialFileSerializable<PersistedFieldSubject> {
    private val file: SequentialFile?
    override var pointer: EntityPointer<PersistedFieldSubject>? = null
    override val serializer: SequentialFileSerializer<PersistedFieldSubject> get() = Serializer

    override val static: Boolean
    override val name: String
    override val descriptor: String
    override val path: List<String>
    override val sourcePath: String

    constructor(
        static: Boolean,
        name: String,
        descriptor: String,
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
        completeDependencies = dependencies
        dependencyPointers = null
    }

    constructor(
        file: SequentialFile,
        static: Boolean,
        name: String,
        descriptor: String,
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
        completeDependencies = null
        dependencyPointers = dependencies
    }

    override val owner: ClassSubject get() {
        throw UnsupportedOperationException("Not implemented access to field's owner!")
    }

    override val childrenSubjects: List<Subject> get() = emptyList()

    private var completeDependencies: List<PersistedDependency>?
    private var dependencyPointers: List<EntityPointer<PersistedDependency>>?


    override val dependencies: List<PersistedDependency> by Delegates.dependenciesLoadDelegate

    object Serializer: SequentialFileSerializer<PersistedFieldSubject> {
        override fun toBytes(file: SequentialFile, entity: PersistedFieldSubject) = dataBytes { data ->
            entity.apply {
                data.writeBoolean(static)
                data.writeString(name)
                data.writeString(descriptor)
                data.writeStringsList(path)
                dependencies.map { file.put(it, PersistedDependency.Serializer) }.let { data.writeEntityPointersList(it) }
                data.writeString(sourcePath)
            }
        }

        override fun fromBytes(file: SequentialFile, bytes: ByteArray): PersistedFieldSubject {
            val data = bytes.getDataInputStream()
            val static = data.readBoolean()
            val name = data.readString()
            val descriptor = data.readString()
            val path = data.readStringList()
            val dependencies = data.readEntityPointersList(PersistedDependency.Serializer)
            val sourcePath = data.readString()

            return PersistedFieldSubject(
                file = file,
                static = static,
                name = name,
                descriptor = descriptor,
                path = path,
                dependencies = dependencies,
                sourcePath = sourcePath
            )
        }
    }

    private object Delegates {
        val dependenciesLoadDelegate = EntitiesListFileLoadDelegate(PersistedFieldSubject::file, PersistedFieldSubject::completeDependencies, PersistedFieldSubject::dependencyPointers)
    }
}