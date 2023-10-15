package com.github.dmitriyushakov.srv_decompiler.cli

import kotlinx.cli.ArgType

data class CommandLineArguments(
    val paths: List<String>,
    val host: String?,
    val port: Int?,
    val sslPort: Int?,
    val sslKeyStore: String?,
    val config: List<String>,
    val commandLineProperties: List<Pair<String, String>>
)
