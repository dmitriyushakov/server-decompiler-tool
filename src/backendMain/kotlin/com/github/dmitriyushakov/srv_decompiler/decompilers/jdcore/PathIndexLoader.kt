package com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import com.github.dmitriyushakov.srv_decompiler.utils.stringsStartIntersectionLength
import org.jd.core.v1.api.loader.Loader

class PathIndexLoader(val pathIndex: PathIndex<Subject>, val sourcePath: String?): Loader {
    override fun canLoad(pathStr: String): Boolean {
        val path = pathStr.split('/')
        return pathIndex[path] is ClassSubject
    }

    override fun load(pathStr: String): ByteArray {
        val path = pathStr.split('/')
        val classSubject = if (sourcePath == null) {
            pathIndex[path].firstNotNullOfOrNull { it as? ClassSubject } ?: error("Unable to load class by path \"$pathStr\"")
        } else {
            pathIndex[path].mapNotNull { it as? ClassSubject }
                .maxByOrNull { stringsStartIntersectionLength(it.sourcePath, sourcePath) }
                ?: error("Unable to load class by path \"$pathStr\" and source \"$sourcePath\"")
        }

        return classSubject.readingContext.use { inputStream ->
            inputStream.readBytes()
        }
    }
}