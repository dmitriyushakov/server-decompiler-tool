package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.model.DependencyItem
import com.github.dmitriyushakov.srv_decompiler.frontend.model.DependencyType

data class DeclarationContextMenuState(
    val incomingRefs: List<Pair<DependencyType, List<DependencyItem>>>,
    val outgoingRefs: List<Pair<DependencyType, List<DependencyItem>>>
)
