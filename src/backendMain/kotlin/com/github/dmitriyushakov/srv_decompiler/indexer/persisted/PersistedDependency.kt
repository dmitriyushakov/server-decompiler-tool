package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.DependencyType
import com.github.dmitriyushakov.srv_decompiler.utils.data.*

class PersistedDependency(
    override val fromPath: List<String>,
    override val toPath: List<String>,
    override val type: DependencyType
) : Dependency {
    private var pointer: EntityPointer<PersistedDependency>? = null

    object Serializer: SequentialFileSerializer<PersistedDependency> {
        private val byteToType: Map<Byte, DependencyType>
        private val typeToByte: Map<DependencyType, Byte>

        init {
            val typeToByteList: List<Pair<DependencyType, Byte>> = DependencyType.values().mapIndexed { idx, type -> type to idx.toByte() }
            typeToByte = typeToByteList.toMap()
            byteToType = typeToByteList.associate { it.second to it.first }
        }

        override fun toBytes(file: SequentialFile, offsetGetter: () -> Long, entity: PersistedDependency): SequentialFileSerializer.Result<PersistedDependency> {
            val pointer = entity.pointer
            if (pointer != null) return pointer.toResult()

            val bytes = dataBytes { data ->
                data.writeStringsList(entity.fromPath)
                data.writeStringsList(entity.toPath)
                data.writeByte(typeToByte[entity.type]!!.toInt())
            }

            entity.pointer = EntityPointer(offsetGetter(), bytes.size, this)
            return bytes.toResult()
        }

        override fun fromBytes(file: SequentialFile, offset: Long, bytes: ByteArray): PersistedDependency {
            val data = bytes.getDataInputStream()
            val fromPath = data.readStringList()
            val toPath = data.readStringList()
            val typeByte = data.readByte()
            val type = byteToType[typeByte] ?: error("Unable to decode byte $typeByte to dependency type!")

            val dep = PersistedDependency(
                fromPath = fromPath,
                toPath = toPath,
                type = type
            )
            dep.pointer = EntityPointer(offset, bytes.size, this)

            return dep
        }
    }
}