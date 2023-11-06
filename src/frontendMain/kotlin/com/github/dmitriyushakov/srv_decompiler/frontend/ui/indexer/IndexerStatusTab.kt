package com.github.dmitriyushakov.srv_decompiler.frontend.ui.indexer

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.IndexerStatus
import com.github.dmitriyushakov.srv_decompiler.frontend.ui.tabs.BasicTab
import io.kvision.core.CssSize
import io.kvision.core.UNIT
import io.kvision.core.style
import io.kvision.html.div
import io.kvision.state.ObservableValue
import io.kvision.state.bind

class IndexerStatusTab: BasicTab("indexer-status-tab") {
    private val indexerStatusObservableValue: ObservableValue<IndexerStatus?> = ObservableValue(IndexerStatus(
        running = false,
        finished = false,
        currentPath = null,
        fileNumber = null,
        filesCount = null
    ))
    private var opened = false

    var openRequest: (() -> Unit)? = null
    var onFinished: (() -> Unit)? = null
    override val label get() = "Index progress"
    override val icon get() = "fa-solid fa-bars-progress"
    init {
        bind(indexerStatusObservableValue) { status ->
            if (status != null) {
                div("Parsing path - ${status.currentPath ?: ""}")
                div("Files - ${status.fileNumber} / ${status.filesCount}")

                div(className = "progress") {
                    val fileNumber = status.fileNumber
                    val filesCount = status.filesCount
                    if (fileNumber != null && filesCount != null) div(className = "progress-bar") {
                        setAttribute("role", "progressbar")
                        setAttribute("aria-valuenow", status.fileNumber.toString())
                        setAttribute("aria-valuemin", "0")
                        setAttribute("aria-valuemax", status.filesCount.toString())
                        style {
                            width = CssSize(100.0 * status.fileNumber.toDouble() / status.filesCount.toDouble(), UNIT.perc)
                        }
                    }
                }
            }
        }

        API.Indexer.receiveStatus { status ->
            indexerStatusObservableValue.setState(status)
            if (status.running && !opened) {
                opened = true
                openRequest?.invoke()
            }
            if (opened && status.finished) {
                onFinished?.invoke()
            }
        }
    }
}