package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import com.github.dmitriyushakov.srv_decompiler.utils.data.*

private const val IS_PUBLIC_FLAG    = 0b000001
private const val IS_PRIVATE_FLAG   = 0b000010
private const val IS_PROTECTED_FLAG = 0b000100
private const val IS_ABSTRACT_FLAG  = 0b001000
private const val IS_INTERFACE_FLAG = 0b010000
private const val IS_FINAL_FLAG     = 0b100000

class PersistedClassSubject : ClassSubject {
    private var pointer: EntityPointer<PersistedClassSubject>? = null

    override val name: String
    override val isPublic: Boolean
    override val isPrivate: Boolean
    override val isProtected: Boolean
    override val isAbstract: Boolean
    override val isInterface: Boolean
    override val isFinal: Boolean
    override val path: List<String>
    override val sourcePath: String

    private val file: SequentialFile?
    private var completeFields: List<PersistedFieldSubject>?
    private var completeMethods: List<PersistedMethodSubject>?
    private var completeReadingContext: ReadingContext?
    private var completeDependencies: List<PersistedDependency>?
    private var fieldsPointers: List<EntityPointer<PersistedFieldSubject>>?
    private var methodsPointers: List<EntityPointer<PersistedMethodSubject>>?
    private var readingContextPointer: EntityPointer<ReadingContext>?
    private var dependencyPointers: List<EntityPointer<PersistedDependency>>?

    constructor(
        name: String,
        isPublic: Boolean,
        isPrivate: Boolean,
        isProtected: Boolean,
        isAbstract: Boolean,
        isInterface: Boolean,
        isFinal: Boolean,
        fields: List<PersistedFieldSubject>,
        methods: List<PersistedMethodSubject>,
        readingContext: ReadingContext,
        path: List<String>,
        dependencies: List<PersistedDependency>,
        sourcePath: String
    ) {
        this.name = name
        this.isPublic = isPublic
        this.isPrivate = isPrivate
        this.isProtected = isProtected
        this.isAbstract = isAbstract
        this.isInterface = isInterface
        this.isFinal = isFinal
        this.path = path
        this.sourcePath = sourcePath

        completeFields = fields
        completeMethods = methods
        completeReadingContext = readingContext
        completeDependencies = dependencies

        file = null
        fieldsPointers = null
        methodsPointers = null
        readingContextPointer = null
        dependencyPointers = null
    }


    constructor(
        file: SequentialFile,
        name: String,
        isPublic: Boolean,
        isPrivate: Boolean,
        isProtected: Boolean,
        isAbstract: Boolean,
        isInterface: Boolean,
        isFinal: Boolean,
        fields: List<EntityPointer<PersistedFieldSubject>>,
        methods: List<EntityPointer<PersistedMethodSubject>>,
        readingContext: EntityPointer<ReadingContext>,
        path: List<String>,
        dependencies: List<EntityPointer<PersistedDependency>>,
        sourcePath: String
    ) {
        this.name = name
        this.isPublic = isPublic
        this.isPrivate = isPrivate
        this.isProtected = isProtected
        this.isAbstract = isAbstract
        this.isInterface = isInterface
        this.isFinal = isFinal
        this.path = path
        this.sourcePath = sourcePath

        completeFields = null
        completeMethods = null
        completeReadingContext = null
        completeDependencies = null

        this.file = file
        fieldsPointers = fields
        methodsPointers = methods
        readingContextPointer = readingContext
        dependencyPointers = dependencies
    }

    override val fields: List<PersistedFieldSubject> by Delegates.fieldsLoadDelegate
    override val methods: List<PersistedMethodSubject> by Delegates.methodsLoadDelegate
    override val readingContext: ReadingContext by Delegates.readingContextLoadDelegate
    override val dependencies: List<PersistedDependency> by Delegates.dependenciesLoadDelegate

    object Serializer: SequentialFileSerializer<PersistedClassSubject> {
        private infix fun Int.isSet(mask: Int): Boolean = (this and mask) != 0
        private infix fun Boolean.to(mask: Int): Int = if (this) mask else 0
        override fun toBytes(file: SequentialFile, offset: Long, entity: PersistedClassSubject): SequentialFileSerializer.Result<PersistedClassSubject> {
            val pointer = entity.pointer
            if (pointer != null) return pointer.toResult()

            val bytes = dataBytes { data ->
                entity.apply {
                    data.writeString(name)
                    val flags = (
                            (isPublic to IS_PUBLIC_FLAG) or
                            (isPrivate to IS_PRIVATE_FLAG) or
                            (isProtected to IS_PROTECTED_FLAG) or
                            (isAbstract to IS_ABSTRACT_FLAG) or
                            (isInterface to IS_INTERFACE_FLAG) or
                            (isFinal to IS_FINAL_FLAG))
                    data.writeByte(flags)
                    entity.fields.map { file.put(it, PersistedFieldSubject.Serializer) }.let { data.writeEntityPointersList(it) }
                    entity.methods.map { file.put(it, PersistedMethodSubject.Serializer) }.let { data.writeEntityPointersList(it) }
                    file.put(entity.readingContext, ReadingContextSerializer).let { data.writeEntityPointer(it) }
                    data.writeStringsList(entity.path)
                    entity.dependencies.map { file.put(it, PersistedDependency.Serializer) }.let { data.writeEntityPointersList(it) }
                    data.writeString(entity.sourcePath)
                }
            }

            entity.pointer = EntityPointer(offset, bytes.size, this)
            return bytes.toResult()
        }

        override fun fromBytes(file: SequentialFile, offset: Long, bytes: ByteArray): PersistedClassSubject {
            val data = bytes.getDataInputStream()
            val name = data.readString()
            val flags = data.readByte().toInt()
            val fields = data.readEntityPointersList(PersistedFieldSubject.Serializer)
            val methods = data.readEntityPointersList(PersistedMethodSubject.Serializer)
            val readingContext = data.readEntityPointer(ReadingContextSerializer)
            val path = data.readStringList()
            val dependencies = data.readEntityPointersList(PersistedDependency.Serializer)
            val sourcePath = data.readString()

            val subject = PersistedClassSubject(
                file = file,
                name = name,
                isPublic = flags isSet IS_PUBLIC_FLAG,
                isPrivate = flags isSet IS_PRIVATE_FLAG,
                isProtected = flags isSet IS_PROTECTED_FLAG,
                isAbstract = flags isSet IS_ABSTRACT_FLAG,
                isInterface = flags isSet IS_INTERFACE_FLAG,
                isFinal = flags isSet  IS_FINAL_FLAG,
                fields = fields,
                methods = methods,
                readingContext = readingContext,
                path = path,
                dependencies = dependencies,
                sourcePath = sourcePath
            )

            subject.pointer = EntityPointer(offset, bytes.size, this)

            return subject
        }

    }

    private object Delegates {
        val fieldsLoadDelegate = EntitiesListFileLoadDelegate(PersistedClassSubject::file, PersistedClassSubject::completeFields, PersistedClassSubject::fieldsPointers)
        val methodsLoadDelegate = EntitiesListFileLoadDelegate(PersistedClassSubject::file, PersistedClassSubject::completeMethods, PersistedClassSubject::methodsPointers)
        val readingContextLoadDelegate = EntityFileLoadDelegate(PersistedClassSubject::file, PersistedClassSubject::completeReadingContext, PersistedClassSubject::readingContextPointer)
        val dependenciesLoadDelegate = EntitiesListFileLoadDelegate(PersistedClassSubject::file, PersistedClassSubject::completeDependencies, PersistedClassSubject::dependencyPointers)
    }
}