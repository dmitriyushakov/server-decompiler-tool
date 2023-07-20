package com.github.dmitriyushakov.srv_decompiler.highlight

import com.github.dmitriyushakov.srv_decompiler.registry.Path

data class LinkGroup (
    val groupType: LinkGroupType,
    val pathLinks: List<Path>
)