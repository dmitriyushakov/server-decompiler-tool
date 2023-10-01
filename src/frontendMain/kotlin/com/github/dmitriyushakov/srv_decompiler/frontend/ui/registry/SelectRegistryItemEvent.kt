package com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry

import com.github.dmitriyushakov.srv_decompiler.frontend.model.ItemType
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path

data class SelectRegistryItemEvent(
    val path: Path,
    val sourcePathList: List<String>,
    val itemType: ItemType
)
