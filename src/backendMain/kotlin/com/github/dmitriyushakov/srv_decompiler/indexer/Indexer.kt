package com.github.dmitriyushakov.srv_decompiler.indexer

import com.github.dmitriyushakov.srv_decompiler.cli.cli
import com.github.dmitriyushakov.srv_decompiler.common.Interner
import com.github.dmitriyushakov.srv_decompiler.indexer.asm.ClassIndexVisitor
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.reading_context.FileReadingContext
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import com.github.dmitriyushakov.srv_decompiler.reading_context.ZipEntryReadingContext
import com.github.dmitriyushakov.srv_decompiler.registry.FileBasedIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.IndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.MergedTreeIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

private class Indexer

private val logger = LoggerFactory.getLogger(Indexer::class.java)

private const val CLASS_EXT = ".class"
private const val JAR_EXT = ".jar"
private const val WAR_EXT = ".war"

data class IndexerStatus(
    val running: Boolean,
    val finished: Boolean,
    val currentPath: String?,
    val fileNumber: Int?,
    val filesCount: Int?
)

var indexerStatus = IndexerStatus(
    running = false,
    finished = false,
    currentPath = null,
    fileNumber = null,
    filesCount = null
)

private fun scanIndexingTargets(paths: List<String>): List<IndexingTarget> {
    val targets: MutableList<IndexingTarget> = mutableListOf()
    val scanningQueue = ArrayDeque<File>()
    scanningQueue.addAll(paths.map(::File))

    while (scanningQueue.isNotEmpty()) {
        val file = scanningQueue.removeFirst()

        if (file.isDirectory) {
            val filesList = file.listFiles()
            if (filesList != null) {
                scanningQueue.addAll(filesList)
            } else {
                logger.warn("Unable to list directory \"${file.path}\"")
            }
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

private fun IndexRegistry.indexForClass(inputStream: InputStream, readingContext: ReadingContext, stringInterner: Interner<String>) {
    try {
        val classVisitor = ClassIndexVisitor(Opcodes.ASM9, readingContext)
        val classReader = ClassReader(inputStream)
        classReader.accept(classVisitor, ClassReader.SKIP_FRAMES)
        val classSubject = classVisitor.toClassSubject(stringInterner)

        indexForSubject(classSubject)
    } catch (ex: Exception) {
        logger.error("Exception occurred during indexing class \"${readingContext.readingDataPath}\" - ", ex)
    }
}

private fun IndexRegistry.indexForClassFile(path: String, stringInterner: Interner<String>) {
    FileInputStream(path).use { fileInputStream ->
        val bufferedFileInputStream = BufferedInputStream(fileInputStream)
        indexForClass(bufferedFileInputStream, FileReadingContext(path), stringInterner)
    }
}

private fun IndexRegistry.indexForZipInputStream(zipInputStream: ZipInputStream, readingContext: ReadingContext, stringInterner: Interner<String>) {
    var entry = zipInputStream.getNextEntry()

    while (entry != null) {
        val lowerEntryName = entry.name.lowercase().let(stringInterner)
        val zipEntryReadingContext = ZipEntryReadingContext(entry.name, readingContext)

        if (lowerEntryName.endsWith(CLASS_EXT)) {
            indexForClass(zipInputStream, zipEntryReadingContext, stringInterner)
        } else if(lowerEntryName.endsWith(JAR_EXT) || lowerEntryName.endsWith(WAR_EXT)) {
            val nestedZipInputStream = ZipInputStream(zipInputStream)
            indexForZipInputStream(nestedZipInputStream, zipEntryReadingContext, stringInterner)
        }

        zipInputStream.closeEntry()
        entry = zipInputStream.getNextEntry()
    }
}

private fun IndexRegistry.indexForJarFile(path: String, stringInterner: Interner<String>) {
    FileInputStream(path).use { fileInputStream ->
        val bufferedFileInputStream = BufferedInputStream(fileInputStream)

        ZipInputStream(bufferedFileInputStream).use { zipInputStream ->
            indexForZipInputStream(zipInputStream, FileReadingContext(path), stringInterner)
        }
    }
}

private fun indexClasses(newRegistry: IndexRegistry, paths: List<String>) {
    indexerStatus = IndexerStatus(
        running = true,
        finished = false,
        currentPath = null,
        fileNumber = null,
        filesCount = null
    )

    val indexingTargets = scanIndexingTargets(paths)
    val targetsCount = indexingTargets.size

    val stringInterner: Interner<String> = Interner()

    for ((idx, target) in indexingTargets.withIndex()) {
        if (logger.isDebugEnabled) {
            val fileType = when(target.type) {
                IndexingTargetType.ClassFile -> "class file"
                IndexingTargetType.JarFile -> "JAR file"
                IndexingTargetType.WarFile -> "WAR file"
            }

            logger.debug("Indexing for {}, path - {}", fileType, target.path)
        }

        indexerStatus = IndexerStatus(
            running = true,
            finished = false,
            currentPath = target.path,
            fileNumber = idx,
            filesCount = targetsCount
        )

        when (target.type) {
            IndexingTargetType.ClassFile -> newRegistry.indexForClassFile(target.path, stringInterner)
            IndexingTargetType.WarFile, IndexingTargetType.JarFile -> newRegistry.indexForJarFile(target.path, stringInterner)
        }
    }

    indexerStatus = IndexerStatus(
        running = false,
        finished = true,
        currentPath = null,
        fileNumber = targetsCount,
        filesCount = targetsCount
    )
}

private fun indexToInMemoryIndex(paths: List<String>) {
    val newRegistry = MergedTreeIndexRegistry()
    indexClasses(newRegistry, paths)
    globalIndexRegistry = newRegistry
}

private fun indexToFileBasedIndex(paths: List<String>, temporary: Boolean) {
    val indexFilesPrefix = cli.indexFilesPrefix ?: "./decompiler_index_registry"
    val treeFile = File("$indexFilesPrefix.tree")
    val entitiesFile = File("$indexFilesPrefix.entities")
    val alreadyExists = treeFile.exists() && entitiesFile.exists()
    val newRegistry = FileBasedIndexRegistry(treeFile, entitiesFile, temporary, cli.compressIndex)
    if (!alreadyExists || temporary) {
        try {
            indexClasses(newRegistry, paths)
        } catch (th: Throwable) {
            logger.error("Exception caused during creation of file based index!", th)
            try {
                newRegistry.close()
            } catch (ex: Exception) {
                logger.error("Exception caused during close of file based index!", ex)
            }
            if (!treeFile.delete()) treeFile.deleteOnExit()
            if (!entitiesFile.delete()) entitiesFile.deleteOnExit()
            throw th
        }
    }
    newRegistry.flush()
    globalIndexRegistry = newRegistry
}

fun startIndexation(indexType: IndexType, paths: List<String>) {
    val runnable = Runnable {
        when (indexType) {
            IndexType.InMemory -> indexToInMemoryIndex(paths)
            IndexType.FileBased -> indexToFileBasedIndex(paths, false)
            IndexType.FileBasedTemp -> indexToFileBasedIndex(paths, true)
        }
    }

    val th = Thread(runnable)
    th.start()
}