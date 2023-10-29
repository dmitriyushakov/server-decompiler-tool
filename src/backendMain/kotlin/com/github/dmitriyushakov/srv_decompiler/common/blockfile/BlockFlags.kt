package com.github.dmitriyushakov.srv_decompiler.common.blockfile

private val FIRST_BLOCK_FLAG: Byte = 0b1
private val LAST_BLOCK_FLAG: Byte = 0b10

class BlockFlags(var flagsByte: Byte) {
    constructor(actions: BlockFlags.() -> Unit): this(0) {
        apply(actions)
    }

    var isFirstBlock: Boolean
        get() = (flagsByte.toInt() and FIRST_BLOCK_FLAG.toInt()) != 0
        set(value) {
            if (value) flagsByte = (flagsByte.toInt() and FIRST_BLOCK_FLAG.toInt()).toByte()
            else flagsByte = (flagsByte.toInt() and FIRST_BLOCK_FLAG.toInt().inv()).toByte()
        }

    var isLastBlock: Boolean
        get() = (flagsByte.toInt() and LAST_BLOCK_FLAG.toInt()) != 0
        set(value) {
            if (value) flagsByte = (flagsByte.toInt() and LAST_BLOCK_FLAG.toInt()).toByte()
            else flagsByte = (flagsByte.toInt() and LAST_BLOCK_FLAG.toInt().inv()).toByte()
        }
}