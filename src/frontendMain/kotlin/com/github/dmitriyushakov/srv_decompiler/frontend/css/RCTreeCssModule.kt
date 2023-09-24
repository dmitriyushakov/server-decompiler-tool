package com.github.dmitriyushakov.srv_decompiler.frontend.css

import io.kvision.ModuleInitializer

object RCTreeCssModule: ModuleInitializer {
    override fun initialize() {
        io.kvision.require("rc-tree/assets/index.css")
    }
}