package com.github.dmitriyushakov.srv_decompiler.api.responses

data class SubjectSearchResponse(val items: List<Item>) {
    data class Item(val name: String, val path: String, val itemType: ItemType)
}