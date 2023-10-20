package com.github.dmitriyushakov.srv_decompiler.api

import com.github.dmitriyushakov.srv_decompiler.common.constants.apiPrefix
import com.github.dmitriyushakov.srv_decompiler.indexer.indexerStatus
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.delay

private val indexerStatusPrefix = "indexerStatus"

fun Application.indexerStatusModule() {
    routing {
        webSocket("$apiPrefix/$indexerStatusPrefix/ws") {
            var last = indexerStatus

            sendSerialized(last)

            while (!last.finished) {
                delay(200)
                val next = indexerStatus
                if (last != next) {
                    sendSerialized(next)
                    last = next
                }
            }
        }
    }
}