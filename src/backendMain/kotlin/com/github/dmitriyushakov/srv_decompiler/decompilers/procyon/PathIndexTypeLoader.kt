package com.github.dmitriyushakov.srv_decompiler.decompilers.procyon

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmClassNameToPath
import com.strobel.assembler.metadata.Buffer
import com.strobel.assembler.metadata.ITypeLoader

class PathIndexTypeLoader(val pathIndex: PathIndex<Subject>): ITypeLoader {
    override fun tryLoadType(internalName: String, buffer: Buffer): Boolean {
        val path = asmClassNameToPath(internalName)
        val classSubject = pathIndex[path].firstNotNullOfOrNull { it as? ClassSubject }
            ?: return false

        val classBytes = classSubject.readingContext.use { it.readBytes() }
        buffer.putByteArray(classBytes, 0, classBytes.size)
        buffer.position(0)
        return true
    }
}