package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

interface Scope {
    fun resolveClass(name: String): Path?
    fun resolveMethod(name: String): Path?
    fun resolveField(name: String): Path?
    fun resolveLocalVariable(name: String): Path?
}