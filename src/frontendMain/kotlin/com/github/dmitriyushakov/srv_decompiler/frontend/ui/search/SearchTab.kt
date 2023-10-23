package com.github.dmitriyushakov.srv_decompiler.frontend.ui.search

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.model.SubjectSearchResponse
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.utils.toFAIconClasses
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.stringToPath
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.form.text.text
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.state.ObservableValue
import io.kvision.state.bind

class SearchTab: BasicTab("search-tab") {
    private val searchResults: ObservableValue<SubjectSearchResponse?> = ObservableValue(null)
    override val icon get() = "fa-solid fa-magnifying-glass"
    override val label: String get() = "Search"

    var onSelectSearchItem: ((Path) -> Unit)? = null

    private fun querySearch(text: String) {
        runPromise {
            val results = API.IndexRegistry.searchForName(text)
            searchResults.setState(results)
        }
    }

    init {
        text(label = "Index search request") {
            onEvent {
                keyup = { ev ->
                    if (ev.key == "Enter") {
                        value?.let { querySearch(it) }
                    }
                }
            }
        }
        div(className = "search-results") {
            bind(searchResults) { results ->
                if (results != null) {
                    for (item in results.items) {
                        div(className = "search-result") {
                            span(className = "search-result-icon") {
                                span(className = item.itemType.toFAIconClasses())
                            }
                            span(item.name, className = "search-result-name")

                            onClick {
                                val path = stringToPath(item.path)
                                onSelectSearchItem?.invoke(path)
                            }
                        }
                    }
                }
            }
        }
    }
}