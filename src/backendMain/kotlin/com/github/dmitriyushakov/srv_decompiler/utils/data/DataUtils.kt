package com.github.dmitriyushakov.srv_decompiler.utils.data

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFileSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

fun ByteArray.getDataInputStream(): DataInputStream = DataInputStream(ByteArrayInputStream(this))

fun DataInputStream.readString(): String {
    val stringBytesCnt = readUnsignedShort()
    return String(readNBytes(stringBytesCnt))
}

fun DataInputStream.readStringList(): List<String> {
    val stringsCnt = readUnsignedShort()
    val strings: MutableList<String> = mutableListOf()

    for (i in 0 until stringsCnt) {
        strings.add(readString())
    }

    return strings
}

fun <T> DataInputStream.readEntityPointer(serializer: SequentialFileSerializer<T>): EntityPointer<T> {
    val offset = readLong()
    val size = readInt()
    return EntityPointer(offset, size, serializer)
}

fun <T> DataInputStream.readEntityPointersList(serializer: SequentialFileSerializer<T>): List<EntityPointer<T>> {
    val count = readUnsignedShort()
    val result: MutableList<EntityPointer<T>> = mutableListOf()

    for (i in 0 until count) {
        result.add(readEntityPointer(serializer))
    }

    return result
}

fun DataOutputStream.writeString(string: String) {
    val bytes = string.encodeToByteArray()
    writeShort(bytes.size)
    write(bytes)
}

fun DataOutputStream.writeStringsList(strings: List<String>) {
    writeShort(strings.size)

    for (str in strings) {
        writeString(str)
    }
}

fun DataOutputStream.writeEntityPointer(pointer: EntityPointer<*>) {
    writeLong(pointer.offset)
    writeInt(pointer.size)
}

fun DataOutputStream.writeEntityPointersList(pointers: List<EntityPointer<*>>) {
    writeShort(pointers.size)

    for (pointer in pointers) {
        writeEntityPointer(pointer)
    }
}

fun dataBytes(actions: (DataOutputStream) -> Unit): ByteArray {
    val baos = ByteArrayOutputStream()
    val data = DataOutputStream(baos)
    actions(data)
    return baos.toByteArray()
}