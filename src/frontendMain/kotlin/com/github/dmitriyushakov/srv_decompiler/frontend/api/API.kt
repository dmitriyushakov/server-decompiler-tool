package com.github.dmitriyushakov.srv_decompiler.frontend.api

import com.github.dmitriyushakov.srv_decompiler.frontend.model.*
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private fun makeClient() = HttpClient {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
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

private inline fun <reified T> getObjectsFromWs(url: String, crossinline action: (T) -> Unit, crossinline isFinalPredicate: (T) -> Boolean) {
    runPromise {
        useClient { client ->
            client.webSocket(url) {
                while (true) {
                    val received: T = receiveDeserialized()
                    action(received)
                    if (isFinalPredicate(received)) break
                }
            }
        }
    }
}

object API {
    private fun pathParam(path: Path): Pair<String, Any?> = "path" to path.joinToString("/")
    private const val apiPrefix = "/api"
    private const val apiPrefixWebSockets = "/apiws"
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

        suspend fun listAncestors(path: Path): ListAncestorsResponse =
            get("$apiPrefix/$registryPrefix/listAncestors", mapOf(pathParam(path)))

        suspend fun listSubjectSources(path: Path): SubjectSourcesResponse =
            get("$apiPrefix/$registryPrefix/listSubjectSources", mapOf(pathParam(path)))
    }

    object Decompiler {
        private const val decompilerPrefix = "decompiler"
        private fun getDecompilerGetParams(path: Path, decompiler: String, sourcePath: String?): Map<String, Any?> =
            mutableMapOf(pathParam(path), "decompiler" to decompiler).let { params ->
                if (sourcePath != null) params["source"] = sourcePath
                params
            }

        suspend fun getRawCode(path: Path, decompiler: String, sourcePath: String?): String =
            get("$apiPrefix/$decompilerPrefix/getRawCode", getDecompilerGetParams(path, decompiler, sourcePath))

        suspend fun getDecompilersList(): DecompilersResponse =
            get("$apiPrefix/$decompilerPrefix/decompilers", emptyMap())

        suspend fun getHighlightedCode(path: Path, decompiler: String, sourcePath: String?): CodeHighlight =
            get("$apiPrefix/$decompilerPrefix/getHighlightedCode", getDecompilerGetParams(path, decompiler, sourcePath))
    }

    object Indexer {
        private const val indexerStatusPrefix = "indexerStatus"
        fun receiveStatus(action: (IndexerStatus) -> Unit) {
            getObjectsFromWs("$apiPrefixWebSockets/$indexerStatusPrefix/ws", action, IndexerStatus::finished)
        }
    }
}