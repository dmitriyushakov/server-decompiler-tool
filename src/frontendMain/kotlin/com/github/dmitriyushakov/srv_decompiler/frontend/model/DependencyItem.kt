package com.github.dmitriyushakov.srv_decompiler.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class DependencyItem(val name: String, val path: String, val itemType: ItemType, val dependencyType: DependencyType, val sourcePathList: List<String>)