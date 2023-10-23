package com.github.dmitriyushakov.srv_decompiler.frontend.ui

import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight.CodeHighlightTab
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.indexer.IndexerStatusTab
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry.RegistryTree
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry.registryTree
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.search.SearchTab
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.findHighestClassPath
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.kvision.core.onEvent
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.panel.*
import kotlinx.browser.window

class MainLayout: SimplePanel("main-layout") {
    private lateinit var tabPanelInternal: TabPanel
    private lateinit var registryTreeInternal: RegistryTree

    val registryTree: RegistryTree get() = registryTreeInternal
    val tabPanel: TabPanel get() = tabPanelInternal

    private var windowTitle: String? = null
        set(value) {
            field = value
            if (value == null) {
                window.document.title = "Server Decompiler Tool"
            } else {
                window.document.title = "$value - Server Decompiler Tool"
            }
        }

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
                tab.onLinkClicked = ::openClassForPath
                openTab(tab)
            }
        }
    }

    init {
        splitPanel {
            div {
                hPanel(className = "tool-buttons-panel") {
                    button("") {
                        span(className = "fa-solid fa-magnifying-glass")
                        onClick {
                            val tab = SearchTab().apply {
                                onSelectSearchItem = ::openClassForPath
                            }
                            openTab(tab)
                        }
                    }
                }
                registryTreeInternal = registryTree()
            }
            tabPanelInternal = tabPanel(className = "main-layout-tab-panel") {
                onEvent {
                    changeTab = { ev ->
                        windowTitle = activeTab?.label
                    }
                }
            }
        }

        registryTree.onSelect = { ev -> openClassForPath(ev.path) }

        IndexerStatusTab().let { tab ->
            tab.openRequest = { openTab(tab) }
            tab.onFinished = { registryTree.reloadTree()}
        }
    }

    fun openTab(tab: BasicTab) {
        val kvTab = tabPanel.tab(label = tab.label, icon = tab.icon, closable = true, init = tab.tabInitializer)
        tabPanel.activeTab = kvTab
    }

    val openedTabs: List<BasicTab> get() = tabPanel.getTabs().flatMap { tab ->
        tab.getChildren().mapNotNull { it as? BasicTab }
    }
}