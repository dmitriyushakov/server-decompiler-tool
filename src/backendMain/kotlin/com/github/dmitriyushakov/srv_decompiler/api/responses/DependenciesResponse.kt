package com.github.dmitriyushakov.srv_decompiler.api.responses

import com.github.dmitriyushakov.srv_decompiler.indexer.model.DependencyType

class DependenciesResponse(val items: List<Item>) {
    data class Item(val name: String, val path: String, val itemType: ItemType, val dependencyType: DependencyType, val sourcePathList: List<String>)
}