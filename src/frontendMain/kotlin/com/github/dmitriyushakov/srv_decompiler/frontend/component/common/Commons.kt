package com.github.dmitriyushakov.srv_decompiler.frontend.component.common

import react.Fragment
import react.create
import react.dom.html.ReactHTML
import web.cssom.ClassName

fun reactFontIcon(classes: String) = Fragment.create {
    ReactHTML.span {
        className = ClassName(classes)
    }
}