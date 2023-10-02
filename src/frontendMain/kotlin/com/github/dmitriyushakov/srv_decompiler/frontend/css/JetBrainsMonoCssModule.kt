package com.github.dmitriyushakov.srv_decompiler.frontend.css

import io.kvision.ModuleInitializer

object JetBrainsMonoCssModule: ModuleInitializer {
    override fun initialize() {
        io.kvision.require("jetbrains-mono/css/jetbrains-mono-nl.css")
    }
}