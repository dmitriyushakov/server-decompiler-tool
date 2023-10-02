package com.github.dmitriyushakov.srv_decompiler.frontend

import com.github.dmitriyushakov.srv_decompiler.frontend.css.CustomCssModule
import com.github.dmitriyushakov.srv_decompiler.frontend.css.JetBrainsMonoCssModule
import com.github.dmitriyushakov.srv_decompiler.frontend.css.RCTreeCssModule
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.MainLayout
import io.kvision.*
import io.kvision.panel.root

class App : Application() {
    override fun start() {
        root("app") {
            add(MainLayout())
        }
    }
}

fun main() {
    startApplication(
        ::App,
        module.hot,
        BootstrapModule,
        BootstrapCssModule,
        CoreModule,
        FontAwesomeModule,
        CustomCssModule,
        RCTreeCssModule,
        JetBrainsMonoCssModule
    )
}