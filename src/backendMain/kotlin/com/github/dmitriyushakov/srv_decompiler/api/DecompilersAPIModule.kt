package com.github.dmitriyushakov.srv_decompiler.api

import com.github.dmitriyushakov.srv_decompiler.common.constants.apiPrefix
import com.github.dmitriyushakov.srv_decompiler.decompilers.DecompilersResponse
import com.github.dmitriyushakov.srv_decompiler.decompilers.decompilers
import com.github.dmitriyushakov.srv_decompiler.decompilers.decompilersList
import com.github.dmitriyushakov.srv_decompiler.registry.IndexRegistry
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.statuses.badRequest
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmClassNameToPath
import com.github.dmitriyushakov.srv_decompiler.utils.highlight.sourceToHighlight
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

private const val decompilerPrefix = "decompiler"
private val reg: IndexRegistry get() = globalIndexRegistry

private fun PipelineContext<Unit, ApplicationCall>.getCode(): String {
    val path = call.request.queryParameters["path"]?.let(::asmClassNameToPath)
        ?: badRequest("\"path\" should be specified in request parameters.")

    val decompilerName = call.request.queryParameters["decompiler"]
        ?: badRequest("\"decompiler\" should be specified in request parameters.")

    val decompiler = decompilers[decompilerName]
        ?: badRequest("Unable to find \"$decompilerName\" decompiler.")

    val sourcePath = call.request.queryParameters["source"]

    val code = decompiler.decompile(reg.subjectsIndex, path, sourcePath)

    return code
}

fun Application.decompilersModule() {
    routing {
        get("$apiPrefix/$decompilerPrefix/getRawCode") {
            val code = getCode()

            call.respondText(code)
        }

        get("$apiPrefix/$decompilerPrefix/decompilers") {
            val response = DecompilersResponse.make(
                decompilers = decompilersList
            )

            call.respond(response)
        }

        get("$apiPrefix/$decompilerPrefix/getHighlightedCode") {
            val code = getCode()
            val highlight = sourceToHighlight(code)

            call.respond(highlight)
        }
    }
}