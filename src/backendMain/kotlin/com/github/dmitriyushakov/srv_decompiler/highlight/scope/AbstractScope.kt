package com.github.dmitriyushakov.srv_decompiler.highlight.scope

abstract class AbstractScope(
    private val parentScope: Scope? = null
): Scope {
    override fun resolveClass(name: String): Link? {
        return parentScope?.resolveClass(name)
    }

    override fun resolveMethod(name: String): Link? {
        return parentScope?.resolveMethod(name)
    }

    override fun resolveField(name: String): Link? {
        return parentScope?.resolveField(name)
    }

    override fun resolveFieldType(name: String): Link? {
        return parentScope?.resolveFieldType(name)
    }

    override fun resolveLocalVariable(name: String): Link? {
        return parentScope?.resolveLocalVariable(name)
    }

    override fun resolveLocalVariableType(name: String): Link? {
        return parentScope?.resolveLocalVariableType(name)
    }
}