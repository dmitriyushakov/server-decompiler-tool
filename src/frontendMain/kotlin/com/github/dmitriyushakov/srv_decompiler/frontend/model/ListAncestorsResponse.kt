package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class ListAncestorsResponse(val items: List<Item>) {
    @Serializable
    data class Item(val path: Path, val itemType: ItemType)
}