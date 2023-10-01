package com.github.dmitriyushakov.srv_decompiler.frontend.ui

import com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry.RegistryTree
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry.registryTree
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import io.kvision.html.div
import io.kvision.panel.*

class MainLayout: SimplePanel("main-layout") {
    private lateinit var tabPanelInternal: TabPanel
    private lateinit var registryTreeInternal: RegistryTree

    val registryTree: RegistryTree get() = registryTreeInternal
    val tabPanel: TabPanel get() = tabPanelInternal

    init {
        splitPanel {
            div {
                registryTreeInternal = registryTree()
            }
            tabPanelInternal = tabPanel()
        }
    }

    fun openTab(tab: BasicTab) {
        tabPanel.tab(label = tab.label, icon = tab.icon, closable = true, init = tab.tabInitializer)
    }

    val openedTabs: List<BasicTab> get() = tabPanel.getTabs().flatMap { tab ->
        tab.getChildren().mapNotNull { it as? BasicTab }
    }
}