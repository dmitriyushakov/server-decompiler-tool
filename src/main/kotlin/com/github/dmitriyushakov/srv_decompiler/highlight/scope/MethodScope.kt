package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class MethodScope private constructor(
    parentScope: Scope,
    private val localVarsMap: Map<String, Path>
): AbstractScope(parentScope) {
    override fun resolveLocalVariable(name: String): Path? {
        return localVarsMap[name] ?: super.resolveLocalVariable(name)
    }

    class Builder(private val parentScope: Scope) {
        private val localVarsMap: MutableMap<String, Path> = mutableMapOf()

        fun addLocalVar(name: String, path: Path) {
            localVarsMap[name] = path
        }

        fun build(): MethodScope = MethodScope(parentScope, localVarsMap.toMap())
    }

    fun childBuilder() = Builder(this)
    fun buildChild(actions: Builder.() -> Unit): MethodScope = childBuilder().apply(actions).build()
}