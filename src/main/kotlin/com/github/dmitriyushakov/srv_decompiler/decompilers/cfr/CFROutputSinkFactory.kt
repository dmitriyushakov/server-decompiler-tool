package com.github.dmitriyushakov.srv_decompiler.decompilers.cfr

import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.OutputSinkFactory.SinkType
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass
import org.benf.cfr.reader.api.OutputSinkFactory.Sink

class CFROutputSinkFactory(val decompiledJavaSink: DecompiledJavaSink): OutputSinkFactory {
    override fun getSupportedSinks(sinkType: SinkType, available: Collection<SinkClass>): List<SinkClass> {
        if (sinkType == SinkType.JAVA) {
            return available.filter { it == SinkClass.DECOMPILED }
        } else {
            return emptyList()
        }
    }

    override fun <T : Any?> getSink(sinkType: SinkType, sinkClass: SinkClass): Sink<T> {
        if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
            return decompiledJavaSink as Sink<T>
        } else return DummySink(sinkType, sinkClass)
    }
}