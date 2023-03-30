package com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import org.jd.core.v1.api.loader.Loader

class PathIndexLoader(val pathIndex: PathIndex<Subject>): Loader {
    override fun canLoad(pathStr: String): Boolean {
        val path = pathStr.split('/')
        return pathIndex[path] is ClassSubject
    }

    override fun load(pathStr: String): ByteArray {
        val path = pathStr.split('/')
        val classSubject = pathIndex[path].firstNotNullOfOrNull { it as? ClassSubject }
            ?: error("Unable to load class by path \"$pathStr\"")

        return classSubject.readingContext.use { inputStream ->
            inputStream.readBytes()
        }
    }
}