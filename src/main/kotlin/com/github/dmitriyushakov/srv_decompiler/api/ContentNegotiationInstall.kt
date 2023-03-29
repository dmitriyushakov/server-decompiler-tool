package com.github.dmitriyushakov.srv_decompiler.api

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.contentNegotiationInstallModule() {
    install(ContentNegotiation) {
        jackson()
    }
}