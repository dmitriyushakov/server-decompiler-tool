package com.github.dmitriyushakov.srv_decompiler.decompilers

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex

interface Decompiler {
    fun decompile(pathIndex: PathIndex<Subject>, path: List<String>, sourcePath: String?): String
}