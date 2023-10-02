package com.github.dmitriyushakov.srv_decompiler.frontend.ui.highlight

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.model.highlight.CodeHighlight
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.kvision.state.ObservableValue
import io.kvision.state.bind

class CodeHighlightTab(
    val path: Path,
    override val label: String? = null
): BasicTab("code-highlight-tab") {
    override val icon: String? get() = "fa-solid fa-code"

    val codeHighlight: ObservableValue<CodeHighlight> = ObservableValue(CodeHighlight.empty)

    init {
        bind(codeHighlight) { hl ->
            val view = CodeHighlightView(hl)
            add(view)
        }

        addAfterInsertHook {
            runPromise {
                val response = API.Decompiler.getHighlightedCode(path, "jd_core")
                codeHighlight.setState(response)
            }
        }
    }
}