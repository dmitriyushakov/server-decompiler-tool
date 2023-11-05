package com.github.dmitriyushakov.srv_decompiler.cli

import com.github.dmitriyushakov.srv_decompiler.indexer.IndexType

data class CommandLineArguments(
    val paths: List<String>,
    val host: String?,
    val port: Int?,
    val sslPort: Int?,
    val sslKeyStore: String?,
    val config: List<String>,
    val commandLineProperties: List<Pair<String, String>>,
    val indexType: IndexType,
    val indexFilesPrefix: String?,
    val compressIndex: Boolean
)
