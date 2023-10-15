package com.github.dmitriyushakov.srv_decompiler.indexer

import com.github.dmitriyushakov.srv_decompiler.cli.cli
import io.ktor.server.application.*
import io.ktor.server.config.*

fun Application.indexerModule() {
    val pathsList: List<String> = cli.paths.ifEmpty { null } ?:
        environment.config.propertyOrNull("decompiler.paths")?.let { pathsProp ->
        var pathsList: List<String>
        try {
            pathsList = pathsProp.getList()
        } catch (_: ApplicationConfigurationException) {
            pathsList = pathsProp.getString().let { pathsStr ->
                if (pathsStr.contains(';')) {
                    pathsStr.split(';')
                } else {
                    pathsStr.split(':')
                }
            }
        }

        pathsList
    } ?: listOf(".")

    startIndexation(pathsList)
}