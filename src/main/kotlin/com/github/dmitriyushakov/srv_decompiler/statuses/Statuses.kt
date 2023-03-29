package com.github.dmitriyushakov.srv_decompiler.statuses

import com.github.dmitriyushakov.srv_decompiler.statuses.responses.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

class Statuses

private val logger = LoggerFactory.getLogger(Statuses::class.java)

fun Application.statusesModule() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is StatusException) {
                call.respond(HttpStatusCode.fromValue(cause.status), cause.getErrorResponse())
            } else {
                logger.error("Error occurred during request processing.", cause)
                val resp = ErrorResponse(500, "Internal Server Error", cause.message, cause.stackTraceToString())
                call.respond(HttpStatusCode.InternalServerError, resp)
            }
        }
    }
}