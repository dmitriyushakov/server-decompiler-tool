package com.github.dmitriyushakov.srv_decompiler.frontend.ui

import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight.CodeHighlightTab
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry.RegistryTree
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry.registryTree
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.findHighestClassPath
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.kvision.html.div
import io.kvision.panel.*

class MainLayout: SimplePanel("main-layout") {
    private lateinit var tabPanelInternal: TabPanel
    private lateinit var registryTreeInternal: RegistryTree

    val registryTree: RegistryTree get() = registryTreeInternal
    val tabPanel: TabPanel get() = tabPanelInternal

    fun openClassForPath(path: Path) {
        runPromise {
            val classPath = findHighestClassPath(path) ?: return@runPromise
            val openedTab = openedTabs.mapNotNull { it as? CodeHighlightTab }.firstOrNull { it.path == classPath }
            val highlightObjectPath = if (path == classPath) null else path
            if (openedTab != null) {
                val openedKvTab = tabPanel.findTabWithComponent(openedTab)
                tabPanel.activeTab = openedKvTab
                openedTab.highlightObjectPath = highlightObjectPath
            } else {
                val tab = CodeHighlightTab(classPath, highlightObjectPath, classPath.lastOrNull())
                openTab(tab)
            }
        }
    }

    init {
        splitPanel {
            div {
                registryTreeInternal = registryTree()
            }
            tabPanelInternal = tabPanel(className = "main-layout-tab-panel")
        }

        registryTree.onSelect = { ev -> openClassForPath(ev.path) }
    }

    fun openTab(tab: BasicTab) {
        val kvTab = tabPanel.tab(label = tab.label, icon = tab.icon, closable = true, init = tab.tabInitializer)
        tabPanel.activeTab = kvTab
    }

    val openedTabs: List<BasicTab> get() = tabPanel.getTabs().flatMap { tab ->
        tab.getChildren().mapNotNull { it as? BasicTab }
    }
}