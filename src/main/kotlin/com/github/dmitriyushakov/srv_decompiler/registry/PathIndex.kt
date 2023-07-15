package com.github.dmitriyushakov.srv_decompiler.registry

interface PathIndex<T> {
    fun add(path: Path, value: T)
    operator fun get(path: Path): Collection<T>
    operator fun contains(path: Path): Boolean
    fun getChildItems(path: Path): List<Pair<String, List<T>>>
    fun searchForPath(path: Path, onlyRoot: Boolean = true): List<T>
    fun findTopElement(path: Path, predicate: (Path, T) -> Boolean): Pair<Path, T>?
}