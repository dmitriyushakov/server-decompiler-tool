package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class MethodScope private constructor(
    parentScope: Scope,
    private val localVarsMap: Map<String, Link>,
    private val localVarsTypeMap: Map<String, Link>
): AbstractScope(parentScope) {
    override fun resolveLocalVariable(name: String): Link? {
        return localVarsMap[name] ?: super.resolveLocalVariable(name)
    }

    override fun resolveLocalVariableType(name: String): Link? {
        return localVarsTypeMap[name] ?: super.resolveLocalVariableType(name)
    }

    class Builder(private val parentScope: Scope) {
        private val localVarsMap: MutableMap<String, Link> = mutableMapOf()
        private val localVarsTypeMap: MutableMap<String, Link> = mutableMapOf()

        fun addLocalVar(name: String, path: Path, typePath: Path?, lineNumber: Int? = null) {
            Link.fromPath(path, LinkType.LocalVar, lineNumber)?.let { link ->
                localVarsMap[name] = link
            }
            if (typePath != null) Link.fromPath(typePath, LinkType.Class)?.let { link ->
                localVarsTypeMap[name] = link
            }
        }

        fun build(): MethodScope = MethodScope(parentScope, localVarsMap.toMap(), localVarsTypeMap.toMap())
    }

    fun childBuilder() = Builder(this)
    fun buildChild(actions: Builder.() -> Unit): MethodScope = childBuilder().apply(actions).build()
}