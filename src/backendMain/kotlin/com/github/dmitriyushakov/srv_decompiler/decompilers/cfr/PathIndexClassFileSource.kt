package com.github.dmitriyushakov.srv_decompiler.decompilers.cfr

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmClassNameToPath
import com.github.dmitriyushakov.srv_decompiler.utils.stringsStartIntersectionLength
import org.benf.cfr.reader.api.ClassFileSource
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(PathIndexClassFileSource::class.java)
private const val CLASS_EXT = ".class"

class PathIndexClassFileSource(val pathIndex: PathIndex<Subject>, val sourcePath: String?): ClassFileSource {
    private fun String.cutClassExtension(): String =
        if (endsWith(CLASS_EXT)) {
            substring(0, length - CLASS_EXT.length)
        } else {
            this
        }

    override fun informAnalysisRelativePathDetail(usePath: String?, classFilePath: String?) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "Class File Source method called - informAnalysisRelativePathDetail. Use path - {}, class file path - {}",
                usePath,
                classFilePath
            )
        }
    }

    override fun addJar(jarPath: String): Collection<String> {
        if (logger.isDebugEnabled) {
            logger.debug("Class File Source method called - addJar. JAR path - {}", jarPath)
        }

        return emptyList()
    }

    override fun getPossiblyRenamedPath(path: String): String {
        if (logger.isDebugEnabled) {
            logger.debug("Class File Source method called - getPossiblyRenamedPath. Path - {}", path)
        }

        return path
    }

    override fun getClassFileContent(path: String): Pair<ByteArray, String> {
        if (logger.isDebugEnabled) {
            logger.debug("Class File Source method called - getClassFileContent. Path - {}", path)
        }

        val indexPath = asmClassNameToPath(path.cutClassExtension())
        val classSubject = if (sourcePath == null) {
            pathIndex[indexPath].firstNotNullOfOrNull { it as? ClassSubject } ?: error("Unable to load class by path \"$path\"")
        } else {
            pathIndex[indexPath].mapNotNull { it as? ClassSubject }
                .maxByOrNull { stringsStartIntersectionLength(it.sourcePath, sourcePath) }
                ?: error("Unable to load class by path \"$path\" and source \"$sourcePath\"")
        }

        val bytes = classSubject.readingContext.use { it.readBytes() }
        return Pair(bytes, path)
    }
}