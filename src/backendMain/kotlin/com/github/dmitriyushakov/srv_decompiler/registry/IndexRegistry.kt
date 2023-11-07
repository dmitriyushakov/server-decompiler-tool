package com.github.dmitriyushakov.srv_decompiler.registry

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

interface IndexRegistry {
    val subjectsIndex: PathIndex<Subject>
    val outgoingDependenciesIndex: PathIndex<Dependency>
    val incomingDependenciesIndex: PathIndex<Dependency>

    fun commit()
}