package com.github.dmitriyushakov.srv_decompiler.frontend

import io.kvision.Application
import io.kvision.CoreModule
import io.kvision.BootstrapModule
import io.kvision.BootstrapCssModule
import io.kvision.html.div
import io.kvision.module
import io.kvision.panel.root
import io.kvision.startApplication

class App : Application() {
    override fun start() {
        root("kvapp") {
            div("Hello world")
            // TODO
        }
    }
}

fun main() {
    startApplication(
        ::App,
        module.hot,
        BootstrapModule,
        BootstrapCssModule,
        CoreModule
    )
}