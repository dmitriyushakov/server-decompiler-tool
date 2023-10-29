package com.github.dmitriyushakov.srv_decompiler.common.blockfile

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val BLOCK_SIZE: Int = 512
private const val BLOCK_FLAGS_OFFSET: Int = 0
private const val DATA_LENGTH_OFFSET: Int = BLOCK_FLAGS_OFFSET + Byte.SIZE_BYTES
private const val NEXT_BLOCK_INDEX_OFFSET: Int = DATA_LENGTH_OFFSET + Int.SIZE_BYTES
private const val PAYLOAD_OFFSET: Int = NEXT_BLOCK_INDEX_OFFSET + Int.SIZE_BYTES
private const val PAYLOAD_SIZE: Int = BLOCK_SIZE - PAYLOAD_OFFSET

class BlockFile(file: File, isTemp: Boolean = true) {
    private val lock: Lock = ReentrantLock()
    private val raf: RandomAccessFile
    private val buffer: ByteArray = ByteArray(BLOCK_SIZE)

    constructor(filename: String, isTemp: Boolean = true): this(File(filename), isTemp)

    init {
        raf = RandomAccessFile(file, "rw")
        if (isTemp) {
            raf.setLength(0L)
            file.deleteOnExit()
        }
    }

    private fun putInt(offset: Int, value: Int) {
        buffer[offset + 0] = (value ushr 24).toByte()
        buffer[offset + 1] = (value ushr 16).toByte()
        buffer[offset + 2] = (value ushr  8).toByte()
        buffer[offset + 3] = (value ushr  0).toByte()
    }

    private fun getInt(offset: Int): Int {
        val ch1 = buffer[offset + 0].toInt()
        val ch2 = buffer[offset + 1].toInt()
        val ch3 = buffer[offset + 2].toInt()
        val ch4 = buffer[offset + 3].toInt()

        return (ch1 shl 24) or (ch2 shl 16) or (ch3 shl 8) or (ch4 shl 0)
    }

    fun put(payload: ByteArray): Int {
        lock.withLock {
            val blockIndex = (raf.length() / BLOCK_SIZE).toInt()

            put(blockIndex, payload)

            return blockIndex
        }
    }

    private fun getFlags(): BlockFlags = BlockFlags(buffer[BLOCK_FLAGS_OFFSET])

    fun put(blockIndex: Int, payload: ByteArray) {
        lock.withLock {
            var nextBlockIndex = blockIndex
            var payloadIndex = 0
            var firstBlock = true
            while (payloadIndex < payload.size) {
                val offset = nextBlockIndex.toLong() * BLOCK_SIZE
                val dataLength = payload.size - payloadIndex
                val lastBlock = dataLength <= BLOCK_SIZE

                // Step 1. Read old block and get nextBlockIndex from it
                raf.seek(offset)
                val readBytes = raf.read(buffer)

                if (readBytes == BLOCK_SIZE) {
                    // There already block exists at this offset
                    val flags = getFlags()
                    if (firstBlock && !flags.isFirstBlock) error("Write at middle part of another data piece is not allowed!")
                    nextBlockIndex = getInt(NEXT_BLOCK_INDEX_OFFSET)
                } else {
                    nextBlockIndex++
                }

                // Step 2. Write the new block with new data
                buffer[BLOCK_FLAGS_OFFSET] = BlockFlags {
                    isFirstBlock = firstBlock
                    isLastBlock = lastBlock
                }.flagsByte
                putInt(DATA_LENGTH_OFFSET, dataLength)
                putInt(NEXT_BLOCK_INDEX_OFFSET, nextBlockIndex)
                val bytesToCopy = Math.min(dataLength, PAYLOAD_SIZE)
                System.arraycopy(payload, payloadIndex, buffer, PAYLOAD_OFFSET, bytesToCopy)
                payloadIndex += bytesToCopy

                raf.seek(offset)
                raf.write(buffer)

                firstBlock = false
            }
        }
    }

    fun get(blockIndex: Int): ByteArray {
        lock.withLock {
            var resultArrayIndex = 0
            var resultArray: ByteArray? = null
            var nextBlockIndex = blockIndex
            var lastBlock = false

            while (!lastBlock) {
                val offset = nextBlockIndex.toLong() * BLOCK_SIZE

                raf.seek(offset)
                raf.read(buffer)

                val flags = getFlags()
                val dataLength = getInt(DATA_LENGTH_OFFSET)
                nextBlockIndex = getInt(NEXT_BLOCK_INDEX_OFFSET)
                lastBlock = flags.isLastBlock

                if (resultArray == null) {
                    if (!flags.isFirstBlock) error("Read begun not from the first block in the record!")
                    resultArray = ByteArray(dataLength)
                }

                val bytesToCopy = Math.min(dataLength, PAYLOAD_SIZE)
                System.arraycopy(buffer, PAYLOAD_OFFSET, resultArray, resultArrayIndex, bytesToCopy)
                resultArrayIndex += bytesToCopy
            }

            return resultArray ?: error("Invalid state! Result array is null after loop!")
        }
    }
}