package com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight

import kotlinx.serialization.Serializable

@Serializable
data class Token(val tokenType: TokenType, val text: String, val link: Link? = null)