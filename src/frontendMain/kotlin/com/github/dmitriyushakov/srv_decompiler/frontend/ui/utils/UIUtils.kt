package com.github.dmitriyushakov.srv_decompiler.frontend.ui.utils

import com.github.dmitriyushakov.srv_decompiler.frontend.model.ItemType

fun ItemType.toFAIconClasses(): String = when(this) {
    ItemType.Package -> "fa-solid fa-box"
    ItemType.Class -> "fa-solid fa-file"
    ItemType.Field -> "fa-solid fa-hashtag"
    ItemType.Method -> "fa-solid fa-bolt"
    ItemType.LocalVar -> "fa-solid fa-hashtag"
}