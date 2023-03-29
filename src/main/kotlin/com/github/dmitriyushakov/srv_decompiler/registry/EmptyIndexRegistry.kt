package com.github.dmitriyushakov.srv_decompiler.registry

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

object EmptyIndexRegistry: IndexRegistry {
    private class EmptyPathIndex<T>: PathIndex<T> {
        override fun add(path: List<String>, value: T) {
            throw NotImplementedError("Modification of empty index not allowed.")
        }

        override fun get(path: List<String>): Collection<T> = emptyList()
        override fun contains(path: List<String>): Boolean = false
        override fun getChildItems(path: List<String>): List<Pair<String, List<T>>> = emptyList()
        override fun searchForPath(path: Path, onlyRoot: Boolean): List<T> = emptyList()
    }

    override val subjectsIndex: PathIndex<Subject> = EmptyPathIndex()
    override val outgoingDependenciesIndex: PathIndex<Dependency> = EmptyPathIndex()
    override val incomingDependenciesIndex: PathIndex<Dependency> = EmptyPathIndex()
}