package com.github.dmitriyushakov.srv_decompiler.highlight.scope

interface Scope {
    fun resolveClass(name: String): Link?
    fun resolveMethod(name: String): Link?
    fun resolveField(name: String): Link?
    fun resolveFieldType(name: String): Link?
    fun resolveLocalVariable(name: String): Link?
    fun resolveLocalVariableType(name: String): Link?
    fun resolveThis(): Link?
}