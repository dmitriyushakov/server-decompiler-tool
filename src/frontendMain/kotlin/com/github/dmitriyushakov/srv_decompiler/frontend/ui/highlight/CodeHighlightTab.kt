package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise

class CodeHighlightTab(
    val path: Path,
    highlightObjectPath: Path?,
    override val label: String? = null
): BasicTab("code-highlight-tab") {
    var onLinkClicked: ((Path) -> Unit)? = null
    val codeHighlightView: CodeHighlightView
    override val icon: String? get() = "fa-solid fa-code"

    var highlightObjectPath: Path? = highlightObjectPath
        set(value) {
            field = value
            val hl = codeHighlightView.state.codeHighlight
            val lightedLine = hl.declarations.firstOrNull { it.path == value }?.lineNumber
            codeHighlightView.state = CodeHighlightViewState(hl, lightedLine)
        }

    init {
        codeHighlightView = CodeHighlightView()
        codeHighlightView.onLinkClicked = { onLinkClicked?.invoke(it) }
        add(codeHighlightView)

        addAfterInsertHook {
            runPromise {
                val response = API.Decompiler.getHighlightedCode(path, "jd_core")
                val lightedLine = response.declarations.firstOrNull { it.path == highlightObjectPath }?.lineNumber
                codeHighlightView.state = CodeHighlightViewState(response, lightedLine)
            }
        }
    }
}