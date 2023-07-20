package com.github.dmitriyushakov.srv_decompiler.decompilers.procyon

import com.github.dmitriyushakov.srv_decompiler.decompilers.Decompiler
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.pathToString
import com.strobel.decompiler.DecompilerSettings
import com.strobel.decompiler.PlainTextOutput
import com.strobel.decompiler.Decompiler.decompile as decompileByProcyon
import java.io.StringWriter

object ProcyonDecompiler: Decompiler {
    override fun decompile(pathIndex: PathIndex<Subject>, path: List<String>): String {
        val internalName = pathToString(path)
        val decompilerSettings = DecompilerSettings.javaDefaults().apply {
            typeLoader = PathIndexTypeLoader(pathIndex)
            //setPreviewFeaturesEnabled(true)
            includeErrorDiagnostics = false
        }

        val stringWriter = StringWriter()

        decompileByProcyon(
            internalName,
            PlainTextOutput(stringWriter),
            decompilerSettings
        )

        return stringWriter.toString()
    }
}