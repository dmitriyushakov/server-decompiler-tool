package com.github.dmitriyushakov.srv_decompiler.registry

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

class BasicIndexRegistry: IndexRegistry {
    override val subjectsIndex: PathIndex<Subject> = MapTreePathIndex()
    override val outgoingDependenciesIndex: PathIndex<Dependency> = MapTreePathIndex()
    override val incomingDependenciesIndex: PathIndex<Dependency> = MapTreePathIndex()
}