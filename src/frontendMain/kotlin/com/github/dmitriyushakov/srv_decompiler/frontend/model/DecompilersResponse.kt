package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class DecompilersResponse (val decompilers: List<DecompilersListItem>) {
    @Serializable
    data class DecompilersListItem(val name: String, val displayName: String)
}