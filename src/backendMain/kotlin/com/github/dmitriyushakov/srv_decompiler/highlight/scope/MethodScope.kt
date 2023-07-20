package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class MethodScope private constructor(
    parentScope: Scope,
    private val localVarsMap: Map<String, Link>
): AbstractScope(parentScope) {
    override fun resolveLocalVariable(name: String): Link? {
        return localVarsMap[name] ?: super.resolveLocalVariable(name)
    }

    class Builder(private val parentScope: Scope) {
        private val localVarsMap: MutableMap<String, Link> = mutableMapOf()

        fun addLocalVar(name: String, path: Path, lineNumber: Int? = null) {
            Link.fromPath(path, LinkType.LocalVar, lineNumber)?.let { link ->
                localVarsMap[name] = link
            }
        }

        fun build(): MethodScope = MethodScope(parentScope, localVarsMap.toMap())
    }

    fun childBuilder() = Builder(this)
    fun buildChild(actions: Builder.() -> Unit): MethodScope = childBuilder().apply(actions).build()
}