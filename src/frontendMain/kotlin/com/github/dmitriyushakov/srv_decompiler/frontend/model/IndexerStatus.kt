package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexerStatus(
    val running: Boolean,
    val finished: Boolean,
    val currentPath: String?,
    val fileNumber: Int?,
    val filesCount: Int?
)