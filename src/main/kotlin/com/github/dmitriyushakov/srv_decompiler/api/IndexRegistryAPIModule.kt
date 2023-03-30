package com.github.dmitriyushakov.srv_decompiler.api

import com.github.dmitriyushakov.srv_decompiler.api.responses.ItemType
import com.github.dmitriyushakov.srv_decompiler.api.responses.ListPackageResponse
import com.github.dmitriyushakov.srv_decompiler.api.responses.DependenciesResponse
import com.github.dmitriyushakov.srv_decompiler.api.responses.SubjectSearchResponse
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
                    ListPackageResponse.Item(name, ItemType.Package, haveChild)
                } else {
                    val itemType = subjectsList.first().itemType
                    ListPackageResponse.Item(name, itemType, haveChild)
                }
            }.let(::ListPackageResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/listOutgoingDeps") {
            val packagePath = call.getPathParam()

            val response = reg.outgoingDependenciesIndex[packagePath].mapNotNull { dep ->
                val depPath = dep.toPath

                reg.subjectsIndex[depPath].firstOrNull()?.let { subject ->
                    DependenciesResponse.Item(pathToHumanReadableName(subject.path), pathToString(subject.path), subject.itemType)
                }
            }.let(::DependenciesResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/listIncomingDeps") {
            val packagePath = call.getPathParam()

            val response = reg.incomingDependenciesIndex[packagePath].mapNotNull { dep ->
                val depPath = dep.fromPath

                reg.subjectsIndex[depPath].firstOrNull()?.let { subject ->
                    DependenciesResponse.Item(pathToHumanReadableName(subject.path), pathToString(subject.path), subject.itemType)
                }
            }.let(::DependenciesResponse)

            call.respond(response)
        }

        get("$apiPrefix/$registryPrefix/searchForName") {
            val name = call.request.queryParameters["name"]
                ?: badRequest("Request should have \"name\" query parameter.")

            val response = reg.subjectsIndex.searchForHumanReadableName(name).map { subject ->
                SubjectSearchResponse.Item(pathToHumanReadableName(subject.path), pathToString(subject.path), subject.itemType)
            }.let(::SubjectSearchResponse)

            call.respond(response)
        }
    }
}