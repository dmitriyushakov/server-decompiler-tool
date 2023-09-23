package com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs

import io.kvision.panel.Tab

abstract class BasicTab {
    open val label: String? get() = null
    open val icon: String? get() = null
    abstract fun Tab.makeTab()
    val tabInitializer: Tab.() -> Unit get() = { makeTab() }
}