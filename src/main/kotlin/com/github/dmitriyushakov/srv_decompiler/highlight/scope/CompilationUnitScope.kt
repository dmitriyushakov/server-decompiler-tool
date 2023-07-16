package com.github.dmitriyushakov.srv_decompiler.highlight.scope

import com.github.dmitriyushakov.srv_decompiler.registry.Path

class CompilationUnitScope private constructor(
    private val classPathMap: Map<String, Link>,
    private val methodPathMap: Map<String, Link>
): AbstractScope() {
    override fun resolveClass(name: String): Link? {
        return classPathMap[name] ?: super.resolveClass(name)
    }

    override fun resolveMethod(name: String): Link? {
        return methodPathMap[name] ?: super.resolveMethod(name)
    }

    class Builder {
        private val singleTypeImports: MutableMap<String, Link> = mutableMapOf()
        private val importsOnDemand: MutableMap<String, Link> = mutableMapOf()
        private val importsOnDemandConflicts: MutableSet<String> = mutableSetOf()

        private val staticImports: MutableMap<String, Link> = mutableMapOf()
        private val staticImportsOnDemand: MutableMap<String, Link> = mutableMapOf()
        private val staticImportsOnDemandConflicts: MutableSet<String> = mutableSetOf()

        fun addSingleTypeImport(name: String, path: Path) {
            Link.fromPath(path)?.let { link ->
                singleTypeImports[name] = link
            }
        }

        fun addTypeImportOnDemand(name: String, path: Path) {
            if (name in importsOnDemand) {
                importsOnDemandConflicts.add(name)
            } else {
                Link.fromPath(path)?.let { link ->
                    importsOnDemand[name] = link
                }
            }
        }

        fun addSingleTypeStaticImport(name: String, path: Path) {
            Link.fromPath(path)?.let { link ->
                staticImports[name] = link
            }
        }

        fun addStaticImportOnDemand(name: String, path: Path) {
            if (name in staticImportsOnDemand) {
                staticImportsOnDemandConflicts.add(name)
            } else {
                Link.fromPath(path)?.let { link ->
                    staticImportsOnDemand[name] = link
                }
            }
        }

        fun build(): CompilationUnitScope {
            val imports = sequenceOf(
                importsOnDemand.asSequence().filter { it.key !in importsOnDemandConflicts }.map { it.key to it.value },
                singleTypeImports.asSequence().map { it.key to it.value }
            ).flatten().toMap()

            val stImports = sequenceOf(
                staticImportsOnDemand.asSequence().filter { it.key !in staticImportsOnDemandConflicts }.map { it.key to it.value },
                staticImports.asSequence().map { it.key to it.value }
            ).flatten().toMap()

            return CompilationUnitScope(imports, stImports)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
        fun build(actions: Builder.() -> Unit): CompilationUnitScope = builder().apply(actions).build()
    }

    fun classScopeBuilder(): ClassScope.Builder = ClassScope.Builder(this)
}