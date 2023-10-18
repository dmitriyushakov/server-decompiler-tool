package com.github.dmitriyushakov.srv_decompiler.api

import com.github.dmitriyushakov.srv_decompiler.api.responses.*
import com.github.dmitriyushakov.srv_decompiler.common.constants.apiPrefix
import com.github.dmitriyushakov.srv_decompiler.indexer.model.*
import com.github.dmitriyushakov.srv_decompiler.registry.IndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.statuses.badRequest
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmClassNameToPath
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.pathToHumanReadableName
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.pathToString
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.searchForHumanReadableName
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val registryPrefix = "registry"
private val reg: IndexRegistry get() = globalIndexRegistry

private val Subject.itemType: ItemType get() = when (this) {
    is ClassSubject -> ItemType.Class
    is FieldSubject -> ItemType.Field
    is MethodSubject -> ItemType.Method
    is LocalVariableSubject -> ItemType.LocalVar
    else -> error("Unknown subject")
}

private fun ApplicationCall.getPathParam(): List<String> = (request.queryParameters["path"]
    ?: badRequest("Request should have \"path\" query parameter.")).let(::asmClassNameToPath)

fun Application.module() {
    routing {
        get("$apiPrefix/$registryPrefix/listPackage") {
            val packagePath = call.getPathParam()

            val response = reg.subjectsIndex.getChildItems(packagePath).map { (name, subjectsList) ->
                val subPath = packagePath + listOf(name)
                val haveChild = reg.subjectsIndex.getChildItems(subPath).isNotEmpty()

                if (subjectsList.isEmpty()) {
                    ListPackageResponse.Item(name, ItemType.Package, haveChild, emptyList())
                } else {
                    val itemType = subjectsList.first().itemType
                    ListPackageResponse.Item(name, itemType, haveChild, subjectsList.map { it.sourcePath })
                }
            }.let(::ListPackageResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/listOutgoingDeps") {
            val packagePath = call.getPathParam()

            val response = reg.outgoingDependenciesIndex[packagePath].flatMap { dep ->
                reg.subjectsIndex[dep.toPath].map { dep to it }
            }.map { (dep, subject) ->
                subject to DependenciesResponse.Item(pathToHumanReadableName(subject.path), pathToString(subject.path), subject.itemType, dep.type, emptyList())
            }.groupBy {
                it.second
            }.map { (group, list) ->
                DependenciesResponse.Item(group.name, group.path, group.itemType, group.dependencyType, list.map { it.first.sourcePath })
            }.let(::DependenciesResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/listIncomingDeps") {
            val packagePath = call.getPathParam()

            val response = reg.incomingDependenciesIndex[packagePath].flatMap { dep ->
                reg.subjectsIndex[dep.fromPath].map { dep to it }
            }.map { (dep, subject) ->
                subject to DependenciesResponse.Item(pathToHumanReadableName(subject.path), pathToString(subject.path), subject.itemType, dep.type, emptyList())
            }.groupBy {
                it.second
            }.map { (group, list) ->
                DependenciesResponse.Item(group.name, group.path, group.itemType, group.dependencyType, list.map { it.first.sourcePath })
            }.let(::DependenciesResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/searchForName") {
            val name = call.request.queryParameters["name"]
                ?: badRequest("Request should have \"name\" query parameter.")

            val response = reg.subjectsIndex.searchForHumanReadableName(name).groupBy { subject ->
                SubjectSearchResponse.Item(pathToHumanReadableName(subject.path), pathToString(subject.path), subject.itemType, emptyList())
            }.map { (group, list) ->
                SubjectSearchResponse.Item(group.name, group.path, group.itemType, list.map { it.sourcePath })
            }.let(::SubjectSearchResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/listAncestors") {
            val path = call.getPathParam()
            val growingPath: MutableList<String> = mutableListOf()
            val resultList: MutableList<ListAncestorsResponse.Item> = mutableListOf()

            for (pathPart in path) {
                growingPath.add(pathPart)
                val subject = reg.subjectsIndex[growingPath].firstOrNull() ?: continue
                val itemType = subject.itemType
                // Get immutable list instance
                val subjectPath = growingPath.toList()

                resultList.add(ListAncestorsResponse.Item(subjectPath, itemType))
            }

            val response = ListAncestorsResponse(resultList)
            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/listSubjectSources") {
            val path = call.getPathParam()

            val sourcesList = reg.subjectsIndex[path].map { it.sourcePath }
            val response = SubjectSourcesResponse(sourcesList)
            call.respond(response)
        }
    }
}