package com.github.dmitriyushakov.srv_decompiler.decompilers.cfr

import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.OutputSinkFactory.Sink
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(DummySink::class.java)

class DummySink<T>(val sinkType: OutputSinkFactory.SinkType, val sinkClass: OutputSinkFactory.SinkClass): Sink<T> {
    override fun write(sinkable: T?) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "Dummy sink write is called. Sink type - {}, class - {}, message - {}",
                sinkType.name,
                sinkClass.name,
                sinkable?.toString() ?: "null"
            )
        }
    }
}