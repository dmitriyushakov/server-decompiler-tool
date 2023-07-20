package com.github.dmitriyushakov.srv_decompiler.indexer.model

interface Dependency {
    val fromPath: List<String>
    val toPath: List<String>
    val type: DependencyType
}