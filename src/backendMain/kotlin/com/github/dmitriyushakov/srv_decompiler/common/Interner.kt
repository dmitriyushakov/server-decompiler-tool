package com.github.dmitriyushakov.srv_decompiler.common

import java.util.concurrent.ConcurrentHashMap

class Interner<T>: (T) -> T {
    private val interned: MutableMap<T, T> = ConcurrentHashMap()
    fun intern(item: T): T = interned.putIfAbsent(item, item) ?: item
    override fun invoke(item: T) = intern(item)
}