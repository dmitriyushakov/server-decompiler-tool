package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexItem(val name: String, val path: String, val itemType: ItemType)