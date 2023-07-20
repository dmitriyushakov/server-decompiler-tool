package com.github.dmitriyushakov.srv_decompiler.decompilers.cfr

import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.SinkReturns
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(DecompiledJavaSink::class.java)

class DecompiledJavaSink: OutputSinkFactory.Sink<SinkReturns.Decompiled> {
    private var decompiled: SinkReturns.Decompiled? = null

    val javaCode: String get() = decompiled?.java ?: error("Code is not decompiled.")

    override fun write(sinkable: SinkReturns.Decompiled) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "Decompiled java sink called. Package name - {}, class name - {}, java code - {}",
                sinkable.packageName,
                sinkable.className,
                sinkable.java
            )
        }

        decompiled = sinkable
    }
}