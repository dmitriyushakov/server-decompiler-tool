package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import com.github.dmitriyushakov.srv_decompiler.reading_context.FileReadingContext
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import com.github.dmitriyushakov.srv_decompiler.reading_context.ZipEntryReadingContext
import com.github.dmitriyushakov.srv_decompiler.utils.data.dataBytes
import com.github.dmitriyushakov.srv_decompiler.utils.data.getDataInputStream
import com.github.dmitriyushakov.srv_decompiler.utils.data.readString
import com.github.dmitriyushakov.srv_decompiler.utils.data.writeString
import java.io.DataOutputStream

object ReadingContextSerializer: SequentialFileSerializer<ReadingContext> {
    private enum class ReadingContextType(val typeByte: Int) {
        File(0),
        ZipEntry(1);
        companion object {
            private val valuesArr = values()
            fun fromByte(typeByte: Int): ReadingContextType = valuesArr.first { it.typeByte == typeByte }
        }
    }

    private fun visitReadingContextOut(data: DataOutputStream, ctx: ReadingContext) {
        when(ctx) {
            is ZipEntryReadingContext -> {
                visitReadingContextOut(data, ctx.archiveStreamContext)
                data.writeByte(ReadingContextType.ZipEntry.typeByte)
                data.writeString(ctx.entryName)
            }
            is FileReadingContext -> {
                data.writeByte(ReadingContextType.File.typeByte)
                data.writeString(ctx.file.path)
            }
            else -> error("Unknown kind of reading context ${ctx::class.qualifiedName}")
        }
    }

    override fun toBytes(file: SequentialFile, offset:Long, entity: ReadingContext): SequentialFileSerializer.Result<ReadingContext> {
        val bytes = dataBytes { data ->
            visitReadingContextOut(data, entity)
        }

        return bytes.toResult()
    }

    override fun fromBytes(file: SequentialFile, offset:Long, bytes: ByteArray): ReadingContext {
        var ctx: ReadingContext? = null

        val data = bytes.getDataInputStream()
        while (data.available() > 0) {
            val typeByte = data.readUnsignedByte()
            val type = ReadingContextType.fromByte(typeByte)

            when(type) {
                ReadingContextType.File -> {
                    val filePath = data.readString()
                    ctx = FileReadingContext(filePath)
                }
                ReadingContextType.ZipEntry -> {
                    val entryName = data.readString()
                    ctx = ZipEntryReadingContext(entryName, ctx ?: error("Null context is not expected there!"))
                }
            }
        }

        return ctx ?: error("Null context is not expected there!")
    }
}