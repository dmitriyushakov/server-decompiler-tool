package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.DependenciesResponse
import com.github.dmitriyushakov.srv_decompiler.frontend.model.DependencyItem
import com.github.dmitriyushakov.srv_decompiler.frontend.model.DependencyType
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.*
import io.kvision.dropdown.ContextMenu
import io.kvision.dropdown.ddLink
import io.kvision.dropdown.ddLinkDisabled
import io.kvision.dropdown.dropDown
import io.kvision.form.select.select
import io.kvision.panel.hPanel
import io.kvision.state.ObservableValue
import io.kvision.state.bind
import org.w3c.dom.events.MouseEvent

class CodeHighlightTab(
    val path: Path,
    highlightObjectPath: Path?,
    override val label: String? = null
): BasicTab("code-highlight-tab") {
    private val stateObservable: ObservableValue<CodeHighlightTabState?> = ObservableValue(null)
    private val contextMenuStateObservable: ObservableValue<DeclarationContextMenuState?> = ObservableValue(null)
    private val contextMenu: ContextMenu = ContextMenu(this, false, null) {
        bind(contextMenuStateObservable) { state ->
            if (state != null) {
                dropDown("Incoming references", forDropDown = true) {
                    for ((type, refs) in state.incomingRefs) {
                        dropDown(type.displayName, forDropDown = true) {
                            for (ref in refs) {
                                val path = stringToPath(ref.path)
                                ddLink(pathToQualifiedName(path)) {
                                    onClick {
                                        onLinkClicked?.invoke(path)
                                    }
                                }
                            }
                        }
                    }
                    if (state.incomingRefs.isEmpty()) {
                        ddLinkDisabled("No references")
                    }
                }
                dropDown("Outgoing references", forDropDown = true) {
                    for ((type, refs) in state.outgoingRefs) {
                        dropDown(type.displayName, forDropDown = true) {
                            for (ref in refs) {
                                val path = stringToPath(ref.path)
                                ddLink(pathToQualifiedName(path)) {
                                    onClick {
                                        onLinkClicked?.invoke(path)
                                    }
                                }
                            }
                        }
                    }
                    if (state.outgoingRefs.isEmpty()) {
                        ddLinkDisabled("No references")
                    }
                }
            }
        }
    }

    var onLinkClicked: ((Path) -> Unit)? = null
    var codeHighlightView: CodeHighlightView? = null
    override val icon: String? get() = "fa-solid fa-code"

    private fun DependenciesResponse.groupByType(): List<Pair<DependencyType, List<DependencyItem>>> =
        items.groupBy { it.dependencyType }.map { it.key to it.value }.sortedBy { it.first }

    private fun openDeclarationContextMenu(typePath: Path, mouseEvent: MouseEvent) {
        val contextMenu = this.contextMenu ?: return

        runPromise {
            val incomingDeps = API.IndexRegistry.listIncomingDeps(typePath).groupByType()
            val outgoingDeps = API.IndexRegistry.listOutgoingDeps(typePath).groupByType()
            val newState = DeclarationContextMenuState(incomingDeps, outgoingDeps)
            contextMenuStateObservable.setState(newState)

            contextMenu.positionMenu(mouseEvent)
        }
    }

    var highlightObjectPath: Path? = highlightObjectPath
        set(value) {
            field = value
            codeHighlightView?.let { view ->
                val hl = view.state.codeHighlight
                val lightedLine = hl.declarations.firstOrNull { it.path == value }?.lineNumber
                view.state = CodeHighlightViewState(hl, lightedLine)
            }
        }

    init {
        bind(stateObservable) { state ->
            if (state == null) return@bind

            hPanel(className = "code-highlight-tab-panel") {
                val decompilersSelect = select(options = state.decompilers.decompilers.map { it.name to it.displayName }, value = state.selectedDecompiler, label = "Decompiler")

                decompilersSelect.onSelectValue { value ->
                    val oldState = stateObservable.getState() ?: return@onSelectValue
                    val newState = CodeHighlightTabState(oldState.decompilers, value, oldState.subjectSources, oldState.selectedSourcePath)
                    stateObservable.setState(newState)
                }

                val subjectSourcesSelect = select(options = state.subjectSources.map { it to it }, state.selectedSourcePath, label = "Source path")

                subjectSourcesSelect.onSelectValue { value ->
                    val oldState = stateObservable.getState() ?: return@onSelectValue
                    val newState = CodeHighlightTabState(oldState.decompilers, oldState.selectedDecompiler, oldState.subjectSources, value)
                    stateObservable.setState(newState)
                }
            }

            val view = CodeHighlightView()
            codeHighlightView = view
            view.onLinkClicked = { onLinkClicked?.invoke(it) }
            view.onDeclarationContextMenu = this::openDeclarationContextMenu
            add(view)

            runPromise {
                val response = API.Decompiler.getHighlightedCode(path, state.selectedDecompiler, state.selectedSourcePath)
                val lightedLine = response.declarations.firstOrNull { it.path == highlightObjectPath }?.lineNumber
                view.state = CodeHighlightViewState(response, lightedLine)
            }
        }

        runPromise {
            val decompilers = getDecompilers()
            val defaultDecompiler = decompilers.decompilers.first().name

            val subjectSources = API.IndexRegistry.listSubjectSources(path).subjectSources

            val state = CodeHighlightTabState(decompilers, defaultDecompiler, subjectSources, null)
            stateObservable.setState(state)
        }
    }
}