package com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore

import com.github.dmitriyushakov.srv_decompiler.decompilers.Decompiler
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.pathToString
import org.jd.core.v1.ClassFileToJavaSourceDecompiler

object JDCoreDecompiler: Decompiler {
    private val decompiler = ClassFileToJavaSourceDecompiler()

    override fun decompile(pathIndex: PathIndex<Subject>, path: List<String>): String {
        val loader = PathIndexLoader(pathIndex)
        val printer = JDPrinter()
        val pathStr = pathToString(path)
        decompiler.decompile(loader, printer, pathStr)

        return printer.toString()
    }
}