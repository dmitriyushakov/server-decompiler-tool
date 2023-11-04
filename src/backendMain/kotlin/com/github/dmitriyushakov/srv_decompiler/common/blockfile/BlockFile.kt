package com.github.dmitriyushakov.srv_decompiler.common.blockfile

import java.io.File
import java.io.RandomAccessFile
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

private const val BLOCK_SIZE: Int = 512
private const val BLOCK_FLAGS_OFFSET: Int = 0
private const val DATA_LENGTH_OFFSET: Int = BLOCK_FLAGS_OFFSET + Byte.SIZE_BYTES
private const val NEXT_BLOCK_INDEX_OFFSET: Int = DATA_LENGTH_OFFSET + Int.SIZE_BYTES
private const val PAYLOAD_OFFSET: Int = NEXT_BLOCK_INDEX_OFFSET + Int.SIZE_BYTES
private const val PAYLOAD_SIZE: Int = BLOCK_SIZE - PAYLOAD_OFFSET
private const val DIRTY_BLOCKS_TO_FLUSH: Int = 10000
private const val MULTI_BLOCK_FLUSH_COUNT: Int = 40960

class BlockFile(file: File, isTemp: Boolean = true): AutoCloseable {
    private val lock: Lock = ReentrantLock()
    private val raf: RandomAccessFile
    private val buffer: ByteArray = ByteArray(BLOCK_SIZE)
    private var blocksCount: Int
    private val dirtyBlocks = TreeMap<Int, ByteArray>()

    constructor(filename: String, isTemp: Boolean = true): this(File(filename), isTemp)

    init {
        raf = RandomAccessFile(file, "rw")
        if (isTemp) {
            raf.setLength(0L)
            file.deleteOnExit()
        }
        blocksCount = (raf.length() / BLOCK_SIZE).toInt()
    }

    private fun putInt(offset: Int, value: Int) {
        buffer[offset + 0] = (value ushr 24).toByte()
        buffer[offset + 1] = (value ushr 16).toByte()
        buffer[offset + 2] = (value ushr  8).toByte()
        buffer[offset + 3] = (value ushr  0).toByte()
    }

    private fun getInt(offset: Int): Int {
        val ch1 = buffer[offset + 0].toInt() and 0xFF
        val ch2 = buffer[offset + 1].toInt() and 0xFF
        val ch3 = buffer[offset + 2].toInt() and 0xFF
        val ch4 = buffer[offset + 3].toInt() and 0xFF

        return (ch1 shl 24) or (ch2 shl 16) or (ch3 shl 8) or (ch4 shl 0)
    }

    fun put(payload: ByteArray): Int {
        lock.withLock {
            val blockIndex = blocksCount

            put(blockIndex, payload)

            return blockIndex
        }
    }

    private fun loadBlock(index: Int): Boolean {
        lock.withLock {
            if (dirtyBlocks.containsKey(index)) {
                val dirtyBlock = dirtyBlocks[index]!!
                System.arraycopy(dirtyBlock, 0, buffer, 0, BLOCK_SIZE)
                return true
            } else {
                val offset = index.toLong() * BLOCK_SIZE
                raf.seek(offset)
                val readBytes = raf.read(buffer)
                return readBytes == BLOCK_SIZE
            }
        }
    }

    private fun storeBlock(index: Int) {
        lock.withLock {
            if (dirtyBlocks.containsKey(index)) {
                val dirtyBlock = dirtyBlocks[index]!!
                System.arraycopy(buffer, 0, dirtyBlock, 0, BLOCK_SIZE)
            } else {
                val newDirtyBlock = ByteArray(BLOCK_SIZE)
                System.arraycopy(buffer, 0, newDirtyBlock, 0, BLOCK_SIZE)
                dirtyBlocks[index] = newDirtyBlock
            }

            if (dirtyBlocks.size >= DIRTY_BLOCKS_TO_FLUSH) flushDirtyBlocks()
        }
    }

    private fun flushDirtyBlocks() {
        lock.withLock {
            val flushBuffer = ByteArray(MULTI_BLOCK_FLUSH_COUNT * BLOCK_SIZE)
            var currentStartIndex: Int = -1

            for ((idx, dirtyBlock) in dirtyBlocks) {
                if (currentStartIndex != -1 && (idx < currentStartIndex || idx >= currentStartIndex + MULTI_BLOCK_FLUSH_COUNT)) {
                    val bytesToWrite = min(flushBuffer.size, (blocksCount - currentStartIndex) * BLOCK_SIZE)
                    raf.seek(currentStartIndex.toLong() * BLOCK_SIZE)
                    raf.write(flushBuffer, 0, bytesToWrite)
                    currentStartIndex = -1
                }
                if (currentStartIndex == -1) {
                    currentStartIndex = idx
                    raf.seek(idx.toLong() * BLOCK_SIZE)
                    raf.read(flushBuffer)
                }

                val flushBufferOffset = (idx - currentStartIndex) * BLOCK_SIZE
                System.arraycopy(dirtyBlock, 0, flushBuffer, flushBufferOffset, BLOCK_SIZE)
            }

            if (currentStartIndex != -1) {
                val bytesToWrite = min(flushBuffer.size, (blocksCount - currentStartIndex) * BLOCK_SIZE)
                raf.seek(currentStartIndex.toLong() * BLOCK_SIZE)
                raf.write(flushBuffer, 0, bytesToWrite)
            }

            dirtyBlocks.clear()
        }
    }

    fun flush() {
        flushDirtyBlocks()
    }

    private fun getFlags(): BlockFlags = BlockFlags(buffer[BLOCK_FLAGS_OFFSET])

    fun put(blockIndex: Int, payload: ByteArray) {
        lock.withLock {
            var nextBlockIndex = blockIndex
            var payloadIndex = 0
            var firstBlock = true
            while (payloadIndex < payload.size) {
                val currentBlockIndex = nextBlockIndex
                if (nextBlockIndex >= blocksCount) blocksCount = nextBlockIndex + 1
                val dataLength = payload.size - payloadIndex
                val lastBlock = dataLength <= BLOCK_SIZE

                // Step 1. Read old block and get nextBlockIndex from it
                val blockLoaded = loadBlock(currentBlockIndex)

                if (blockLoaded) {
                    // There already block exists at this offset
                    val flags = getFlags()
                    if (firstBlock && !flags.isFirstBlock) error("Write at middle part of another data piece is not allowed!")

                    // Use next block field only if it is not last block.
                    // Last block shouldn't have recorded index of next block.
                    if (flags.isLastBlock) {
                        nextBlockIndex = blocksCount
                    } else {
                        nextBlockIndex = getInt(NEXT_BLOCK_INDEX_OFFSET)
                    }
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

                storeBlock(currentBlockIndex)

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
                loadBlock(nextBlockIndex)

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

    val size: Int get() = (raf.length() / BLOCK_SIZE).toInt()

    override fun close() {
        raf.close()
    }
}