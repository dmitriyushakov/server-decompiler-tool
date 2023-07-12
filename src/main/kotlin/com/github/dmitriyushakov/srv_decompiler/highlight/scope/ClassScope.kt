package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class ClassScope private constructor(
    parentScope: Scope,
    private val classMap: Map<String, Path>,
    private val methodMap: Map<String, Path>,
    private val fieldMap: Map<String, Path>
): AbstractScope(parentScope) {
    override fun resolveClass(name: String): Path? {
        return classMap[name] ?: super.resolveClass(name)
    }

    override fun resolveMethod(name: String): Path? {
        return methodMap[name] ?: super.resolveMethod(name)
    }

    override fun resolveField(name: String): Path? {
        return fieldMap[name] ?: super.resolveField(name)
    }

    class Builder(private val parentScope: Scope) {
        private val classMap: MutableMap<String, Path> = mutableMapOf()
        private val methodMap: MutableMap<String, Path> = mutableMapOf()
        private val fieldMap: MutableMap<String, Path> = mutableMapOf()

        fun addClass(name: String, path: Path) {
            classMap[name] = path
        }

        fun addMethod(name: String, path: Path) {
            methodMap[name] = path
        }

        fun addField(name: String, path: Path) {
            fieldMap[name] = path
        }

        fun build(): ClassScope = ClassScope(parentScope, classMap.toMap(), methodMap.toMap(), fieldMap.toMap())
    }

    fun childScopeBuilder(): Builder = Builder(this)
    fun buildChildScope(actions: Builder.() -> Unit): ClassScope = childScopeBuilder().apply(actions).build()
    fun methodScopeBuilder(): MethodScope.Builder = MethodScope.Builder(this)
    fun buildMethodScope(actions: MethodScope.Builder.() -> Unit): MethodScope = methodScopeBuilder().apply(actions).build()
}