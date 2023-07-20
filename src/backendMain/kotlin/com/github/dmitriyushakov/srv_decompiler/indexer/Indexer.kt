package com.github.dmitriyushakov.srv_decompiler.indexer

import com.github.dmitriyushakov.srv_decompiler.indexer.asm.ClassIndexVisitor
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.reading_context.FileReadingContext
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import com.github.dmitriyushakov.srv_decompiler.reading_context.ZipEntryReadingContext
import com.github.dmitriyushakov.srv_decompiler.registry.BasicIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.IndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

private class Indexer

private val logger = LoggerFactory.getLogger(Indexer::class.java)

private const val CLASS_EXT = ".class"
private const val JAR_EXT = ".jar"
private const val WAR_EXT = ".war"

private fun scanIndexingTargets(paths: List<String>): List<IndexingTarget> {
    val targets: MutableList<IndexingTarget> = mutableListOf()
    val scanningQueue = ArrayDeque<File>()
    scanningQueue.addAll(paths.map(::File))

    while (scanningQueue.isNotEmpty()) {
        val file = scanningQueue.removeFirst()

        if (file.isDirectory) {
            scanningQueue.addAll(file.listFiles())
        } else if(file.isFile) {
            val fileName = file.name
            val fileNameLower = fileName.lowercase()

            if (fileNameLower.endsWith(JAR_EXT)) {
                targets.add(IndexingTarget(file.absolutePath, IndexingTargetType.JarFile))
            } else if (fileNameLower.endsWith(WAR_EXT)) {
                targets.add(IndexingTarget(file.absolutePath, IndexingTargetType.WarFile))
            } else if (fileNameLower.endsWith(CLASS_EXT)) {
                targets.add(IndexingTarget(file.absolutePath, IndexingTargetType.ClassFile))
            }
        }
    }

    return targets
}

private fun IndexRegistry.indexForSubject(subject: Subject) {
    subjectsIndex.add(subject.path, subject)
    for (dep in subject.dependencies) {
        outgoingDependenciesIndex.add(dep.fromPath, dep)
        incomingDependenciesIndex.add(dep.toPath, dep)
    }

    for (child in subject.childrenSubjects) {
        indexForSubject(child)
    }
}

private fun IndexRegistry.indexForClass(inputStream: InputStream, readingContext: ReadingContext) {
    try {
        val classVisitor = ClassIndexVisitor(Opcodes.ASM9, readingContext)
        val classReader = ClassReader(inputStream)
        classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)
        val classSubject = classVisitor.toClassSubject()

        indexForSubject(classSubject)
    } catch (ex: Exception) {
        logger.error("Exception occurred during indexing class", ex)
    }
}

private fun IndexRegistry.indexForClassFile(path: String) {
    FileInputStream(path).use { fileInputStream ->
        val bufferedFileInputStream = BufferedInputStream(fileInputStream)
        indexForClass(bufferedFileInputStream, FileReadingContext(path))
    }
}

private fun IndexRegistry.indexForZipInputStream(zipInputStream: ZipInputStream, readingContext: ReadingContext) {
    var entry = zipInputStream.getNextEntry()

    while (entry != null) {
        val lowerEntryName = entry.name.lowercase()
        val zipEntryReadingContext = ZipEntryReadingContext(entry.name, readingContext)

        if (lowerEntryName.endsWith(CLASS_EXT)) {
            indexForClass(zipInputStream, zipEntryReadingContext)
        } else if(lowerEntryName.endsWith(JAR_EXT) || lowerEntryName.endsWith(WAR_EXT)) {
            val nestedZipInputStream = ZipInputStream(zipInputStream)
            indexForZipInputStream(nestedZipInputStream, zipEntryReadingContext)
        }

        zipInputStream.closeEntry()
        entry = zipInputStream.getNextEntry()
    }
}

private fun IndexRegistry.indexForJarFile(path: String) {
    FileInputStream(path).use { fileInputStream ->
        val bufferedFileInputStream = BufferedInputStream(fileInputStream)

        ZipInputStream(bufferedFileInputStream).use { zipInputStream ->
            indexForZipInputStream(zipInputStream, FileReadingContext(path))
        }
    }
}

fun indexClasses(paths: List<String>) {
    val indexingTargets = scanIndexingTargets(paths)

    val newRegistry: IndexRegistry = BasicIndexRegistry()

    for (target in indexingTargets) {
        if (logger.isDebugEnabled) {
            val fileType = when(target.type) {
                IndexingTargetType.ClassFile -> "class file"
                IndexingTargetType.JarFile -> "JAR file"
                IndexingTargetType.WarFile -> "WAR file"
            }

            logger.debug("Indexing for {}, path - {}", fileType, target.path)
        }

        when (target.type) {
            IndexingTargetType.ClassFile -> newRegistry.indexForClassFile(target.path)
            IndexingTargetType.WarFile, IndexingTargetType.JarFile -> newRegistry.indexForJarFile(target.path)
        }
    }

    globalIndexRegistry = newRegistry
}

fun startIndexation(paths: List<String>) {
    val runnable = Runnable {
        indexClasses(paths)
    }

    val th = Thread(runnable)
    th.start()
}