package com.github.dmitriyushakov.srv_decompiler.frontend.ui

import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import io.kvision.html.div
import io.kvision.panel.*

class MainLayout: SimplePanel("main-layout") {
    private lateinit var tabPanelBack: TabPanel
    val tabPanel: TabPanel get() = tabPanelBack

    init {
        splitPanel {
            div("Packages and classes tree would be there")
            tabPanelBack = tabPanel()
        }
    }

    fun openTab(tab: BasicTab) {
        tabPanel.tab(label = tab.label, icon = tab.icon, closable = true, init = tab.tabInitializer)
    }
}