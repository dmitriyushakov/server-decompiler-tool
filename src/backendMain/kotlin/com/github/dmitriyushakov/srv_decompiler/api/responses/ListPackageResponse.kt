package com.github.dmitriyushakov.srv_decompiler.api.responses

data class ListPackageResponse(val items: List<Item>) {
    data class Item(val name: String, val itemType: ItemType, val haveItemsInside: Boolean, val sourcePathList: List<String>)
}