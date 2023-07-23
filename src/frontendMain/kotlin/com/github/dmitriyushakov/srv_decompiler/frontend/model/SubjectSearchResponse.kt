package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class SubjectSearchResponse(val items: List<IndexItem>)