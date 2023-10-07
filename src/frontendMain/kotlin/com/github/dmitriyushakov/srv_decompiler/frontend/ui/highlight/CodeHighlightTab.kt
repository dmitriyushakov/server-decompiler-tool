package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.kvision.state.ObservableValue
import io.kvision.state.bind

private class CodeHighlightState(
    val codeHighlight: CodeHighlight,
    val lightedLine: Int? = null
)

class CodeHighlightTab(
    val path: Path,
    highlightObjectPath: Path?,
    override val label: String? = null
): BasicTab("code-highlight-tab") {
    private var codeHighlightView: CodeHighlightView? = null
    var onLinkClicked: ((Path) -> Unit)? = null
    override val icon: String? get() = "fa-solid fa-code"

    private val codeHighlightState: ObservableValue<CodeHighlightState> =
        ObservableValue(CodeHighlightState(CodeHighlight.empty))
    var highlightObjectPath: Path? = highlightObjectPath
        set(value) {
            field = value
            val hl = codeHighlightState.getState().codeHighlight
            val lightedLine = hl.declarations.firstOrNull { it.path == value }?.lineNumber
            codeHighlightState.setState(CodeHighlightState(hl, lightedLine))
        }

    init {
        bind(codeHighlightState) { hls ->
            val hl = hls.codeHighlight
            val lightedLine = hls.lightedLine
            val view = CodeHighlightView(hl, lightedLine, codeHighlightView?.scrollTop)
            view.onLinkClicked = { onLinkClicked?.invoke(it) }
            add(view)
            codeHighlightView = view
        }

        addAfterInsertHook {
            runPromise {
                val response = API.Decompiler.getHighlightedCode(path, "jd_core")
                val lightedLine = response.declarations.firstOrNull { it.path == highlightObjectPath }?.lineNumber
                codeHighlightState.setState(CodeHighlightState(response, lightedLine))
            }
        }
    }
}