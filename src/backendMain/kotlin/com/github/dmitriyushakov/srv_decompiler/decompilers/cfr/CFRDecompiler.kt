package com.github.dmitriyushakov.srv_decompiler.decompilers.cfr

import com.github.dmitriyushakov.srv_decompiler.decompilers.Decompiler
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.pathToString
import org.benf.cfr.reader.api.CfrDriver

object CFRDecompiler: Decompiler {
    override fun decompile(pathIndex: PathIndex<Subject>, path: List<String>, sourcePath: String?): String {
        val decompiledJavaSink = DecompiledJavaSink()
        val sinkFactory = CFROutputSinkFactory(decompiledJavaSink)
        val classFileSource = PathIndexClassFileSource(pathIndex, sourcePath)

        val cfrDriver = CfrDriver
            .Builder()
            .withOutputSink(sinkFactory)
            .withClassFileSource(classFileSource)
            .build()

        cfrDriver.analyse(listOf(pathToString(path)))

        return decompiledJavaSink.javaCode
    }
}