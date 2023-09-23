package com.github.dmitriyushakov.srv_decompiler.frontend.css

import io.kvision.ModuleInitializer
import io.kvision.require

object CustomCssModule: ModuleInitializer {
    override fun initialize() {
        require("./css/custom.css")
    }
}