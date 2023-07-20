package com.github.dmitriyushakov.srv_decompiler.decompilers

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex

interface Decompiler: (PathIndex<Subject>, List<String>) -> String{
    override fun invoke(pathIndex: PathIndex<Subject>, path: List<String>): String = decompile(pathIndex, path)
    fun decompile(pathIndex: PathIndex<Subject>, path: List<String>): String
}