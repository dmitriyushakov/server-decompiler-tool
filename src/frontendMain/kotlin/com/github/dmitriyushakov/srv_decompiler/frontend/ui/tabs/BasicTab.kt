package com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs

import io.kvision.panel.SimplePanel
import io.kvision.panel.Tab

abstract class BasicTab(className: String? = null): SimplePanel(className = className) {
    open val label: String? get() = null
    open val icon: String? get() = null

    val tabInitializer: Tab.() -> Unit get() = {
        add(this@BasicTab)
    }
}