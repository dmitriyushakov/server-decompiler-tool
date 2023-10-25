package com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore

import org.jd.core.v1.api.printer.Printer

private const val TAB = "\t"
private const val NEWLINE = "\n"

class JDPrinter: Printer {
    private var indentationCount = 0
    private val sb = StringBuilder()

    override fun toString() = sb.toString()

    override fun start(maxLineNumber: Int, majorVersion: Int, minorVersion: Int) {}
    override fun end() {}

    override fun printText(text: String) {
        sb.append(text)
    }

    override fun printNumericConstant(constant: String) {
        sb.append(constant)
    }

    override fun printStringConstant(constant: String, ownerInternalName: String?) {
        sb.append(constant)
    }

    override fun printKeyword(keyword: String) {
        sb.append(keyword)
    }

    override fun printDeclaration(type: Int, internalTypeName: String?, name: String?, descriptor: String?) {
        sb.append(name)
    }

    override fun printReference(type: Int, internalTypeName: String?, name: String?, descriptor: String?, ownerInternalName: String?) {
        sb.append(name)
    }

    override fun indent() {
        indentationCount ++
    }

    override fun unindent() {
        indentationCount --
    }

    override fun startLine(lineNumber: Int) {
        for (i in 0 until indentationCount) {
            sb.append(TAB)
        }
    }

    override fun endLine() {
        sb.append(NEWLINE)
    }

    override fun extraLine(count: Int) {
        for (i in 0 until count) {
            sb.append(NEWLINE)
        }
    }

    override fun startMarker(type: Int) {}

    override fun endMarker(type: Int) {}
}