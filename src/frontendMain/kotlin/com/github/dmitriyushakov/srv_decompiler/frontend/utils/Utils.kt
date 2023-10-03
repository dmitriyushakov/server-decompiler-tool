package com.github.dmitriyushakov.srv_decompiler.frontend.utils

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.ItemType
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

fun pathToString(path: Path): String = path.joinToString("/")

fun <T> runPromise(block: suspend CoroutineScope.() -> T): Promise<T> {
    return GlobalScope.promise {
        try {
            block()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }
    }
}

suspend fun findHighestClassPath(path: Path): Path? =
    API.IndexRegistry.listAncestors(path).items.firstOrNull { it.itemType == ItemType.Class }?.path