package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class SubjectSourcesResponse(val subjectSources: List<String>)