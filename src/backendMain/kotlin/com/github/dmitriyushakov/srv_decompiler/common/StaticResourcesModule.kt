package com.github.dmitriyushakov.srv_decompiler.common

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.servingStaticResourcesModule() {
    routing {
        staticResources("/", "assets")
    }
}