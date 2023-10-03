package com.github.dmitriyushakov.srv_decompiler.api.responses

import com.github.dmitriyushakov.srv_decompiler.registry.Path

data class ListAncestorsResponse(val items: List<Item>) {
    data class Item(val path: Path, val itemType: ItemType)
}