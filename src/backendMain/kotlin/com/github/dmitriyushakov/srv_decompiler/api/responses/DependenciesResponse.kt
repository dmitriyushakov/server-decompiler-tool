package com.github.dmitriyushakov.srv_decompiler.api.responses

class DependenciesResponse(val items: List<Item>) {
    data class Item(val name: String, val path: String, val itemType: ItemType, val sourcePathList: List<String>)
}