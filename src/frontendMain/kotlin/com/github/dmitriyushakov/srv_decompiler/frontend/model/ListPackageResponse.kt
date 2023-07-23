package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class ListPackageResponse(val items: List<Item>) {
    @Serializable
    data class Item(val name: String, val itemType: ItemType, val haveItemsInside: Boolean)
}