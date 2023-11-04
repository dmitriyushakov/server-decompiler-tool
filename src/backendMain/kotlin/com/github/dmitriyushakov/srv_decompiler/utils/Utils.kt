package com.github.dmitriyushakov.srv_decompiler.utils

import kotlin.math.min

fun stringsStartIntersectionLength(first: String, second: String): Int {
    val length = min(first.length, second.length)
    for (i in 0 until length) {
        if (first[i] != second[i]) return i
    }
    return length
}

fun <K, V> lruCache(maxSize: Int): MutableMap<K, V> = object:LinkedHashMap<K,V>(maxSize * 4 / 3, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
        return size > maxSize
    }
}