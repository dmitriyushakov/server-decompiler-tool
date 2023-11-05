package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializable
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.DependencyType
import com.github.dmitriyushakov.srv_decompiler.utils.data.*

class PersistedDependency(
    override val fromPath: List<String>,
    override val toPath: List<String>,
    override val type: DependencyType
) : Dependency, SequentialFileSerializable<PersistedDependency> {
    override var pointer: EntityPointer<PersistedDependency>? = null
    override val serializer: SequentialFileSerializer<PersistedDependency> get() = Serializer

    object Serializer: SequentialFileSerializer<PersistedDependency> {
        private val byteToType: Map<Byte, DependencyType>
        private val typeToByte: Map<DependencyType, Byte>

        init {
            val typeToByteList: List<Pair<DependencyType, Byte>> = DependencyType.values().mapIndexed { idx, type -> type to idx.toByte() }
            typeToByte = typeToByteList.toMap()
            byteToType = typeToByteList.associate { it.second to it.first }
        }

        override fun toBytes(file: SequentialFile, entity: PersistedDependency) = dataBytes { data ->
            data.writeStringsList(entity.fromPath)
            data.writeStringsList(entity.toPath)
            data.writeByte(typeToByte[entity.type]!!.toInt())
        }

        override fun fromBytes(file: SequentialFile, bytes: ByteArray): PersistedDependency {
            val data = bytes.getDataInputStream()
            val fromPath = data.readStringList()
            val toPath = data.readStringList()
            val typeByte = data.readByte()
            val type = byteToType[typeByte] ?: error("Unable to decode byte $typeByte to dependency type!")

            return PersistedDependency(
                fromPath = fromPath,
                toPath = toPath,
                type = type
            )
        }
    }
}