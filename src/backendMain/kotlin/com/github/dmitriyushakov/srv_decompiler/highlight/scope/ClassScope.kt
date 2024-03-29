package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class ClassScope private constructor(
    parentScope: Scope,
    private val classMap: Map<String, Link>,
    private val methodMap: Map<String, Link>,
    private val fieldMap: Map<String, Link>,
    private val fieldTypeMap: Map<String, Link>
): AbstractScope(parentScope) {
    override fun resolveClass(name: String): Link? {
        return classMap[name] ?: super.resolveClass(name)
    }

    override fun resolveMethod(name: String): Link? {
        return methodMap[name] ?: super.resolveMethod(name)
    }

    override fun resolveField(name: String): Link? {
        return fieldMap[name] ?: super.resolveField(name)
    }

    override fun resolveFieldType(name: String): Link? {
        return fieldTypeMap[name] ?: super.resolveFieldType(name)
    }

    class Builder(private val parentScope: Scope) {
        private val classMap: MutableMap<String, Link> = mutableMapOf()
        private val methodMap: MutableMap<String, Link> = mutableMapOf()
        private val fieldMap: MutableMap<String, Link> = mutableMapOf()
        private val fieldTypeMap: MutableMap<String, Link> = mutableMapOf()

        fun addClass(name: String, path: Path, lineNumber: Int? = null) {
            Link.fromPath(path, LinkType.Class, lineNumber)?.let { link ->
                classMap[name] = link
            }
        }

        fun addMethod(name: String, path: Path, lineNumber: Int? = null) {
            Link.fromPath(path, LinkType.Method, lineNumber)?.let { link ->
                methodMap[name] = link
            }
        }

        fun addField(name: String, path: Path, typePath: Path?, lineNumber: Int? = null) {
            Link.fromPath(path, LinkType.Field, lineNumber)?.let { link ->
                fieldMap[name] = link
            }
            if (typePath != null) Link.fromPath(typePath, LinkType.Class)?.let { link ->
                fieldTypeMap[name] = link
            }
        }

        fun build(): ClassScope = ClassScope(parentScope, classMap.toMap(), methodMap.toMap(), fieldMap.toMap(), fieldTypeMap.toMap())
    }

    fun methodScopeBuilder(): MethodScope.Builder = MethodScope.Builder(this)
    fun buildMethodScope(actions: MethodScope.Builder.() -> Unit): MethodScope = methodScopeBuilder().apply(actions).build()

    companion object {
        fun buildFrom(parentScope: Scope, actions: Builder.() -> Unit): ClassScope = Builder(parentScope).apply(actions).build()
    }
}