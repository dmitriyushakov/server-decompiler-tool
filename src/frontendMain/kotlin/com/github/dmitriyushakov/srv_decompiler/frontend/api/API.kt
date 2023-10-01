package com.github.dmitriyushakov.srv_decompiler.frontend.api

import com.github.dmitriyushakov.srv_decompiler.frontend.model.*
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*

private fun makeClient() = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

private suspend fun <R> useClient(actions: suspend (HttpClient) -> R): R {
    val client = makeClient()

    try {
        return actions(client)
    } finally {
        client.close()
    }
}

private suspend inline fun <reified T> get(url: String, getParams: Map<String, Any?>):T {
    return useClient { client ->
        val response = client.get(url) {
            url {
                for (entry in getParams.entries) {
                    parameters.append(entry.key, entry.value?.toString() ?: "")
                }
            }
        }

        try {
            response.body()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }

    }
}

object API {
    private fun pathParam(path: Path): Pair<String, Any?> = "path" to path.joinToString("/")
    private const val apiPrefix = "/api"
    object IndexRegistry {
        private const val registryPrefix = "registry"

        suspend fun listPackage(path: Path): ListPackageResponse =
            get("$apiPrefix/$registryPrefix/listPackage", mapOf(pathParam(path)))

        suspend fun listOutgoingDeps(path: Path): DependenciesResponse =
            get("$apiPrefix/$registryPrefix/listOutgoingDeps", mapOf(pathParam(path)))

        suspend fun listIncomingDeps(path: Path): DependenciesResponse =
            get("$apiPrefix/$registryPrefix/listIncomingDeps", mapOf(pathParam(path)))

        suspend fun searchForName(name: String): SubjectSearchResponse =
            get("$apiPrefix/$registryPrefix/searchForName", mapOf("name" to name))
    }

    object Decompiler {
        private const val decompilerPrefix = "decompiler"

        suspend fun getRawCode(path: Path, decompiler: String): String =
            get("$apiPrefix/$decompilerPrefix/getRawCode", mapOf(pathParam(path), "decompiler" to decompiler))

        suspend fun getDecompilersList(): DecompilersResponse =
            get("$apiPrefix/$decompilerPrefix/decompilers", emptyMap())

        suspend fun getHighlightedCode(path: Path, decompiler: String): CodeHighlight =
            get("$apiPrefix/$decompilerPrefix/getHighlightedCode", mapOf(pathParam(path), "decompiler" to decompiler))
    }
}