package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

abstract class AbstractScope(
    private val parentScope: Scope? = null
): Scope {
    override fun resolveClass(name: String): Path? {
        return parentScope?.resolveClass(name)
    }

    override fun resolveMethod(name: String): Path? {
        return parentScope?.resolveMethod(name)
    }

    override fun resolveField(name: String): Path? {
        return parentScope?.resolveField(name)
    }

    override fun resolveLocalVariable(name: String): Path? {
        return parentScope?.resolveLocalVariable(name)
    }
}