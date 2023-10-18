package com.github.dmitriyushakov.srv_decompiler.frontend.utils

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.model.DecompilersResponse
import com.github.dmitriyushakov.srv_decompiler.frontend.model.ItemType
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import io.kvision.core.onChange
import io.kvision.form.select.Select
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

private var cachedDecompilers: DecompilersResponse? = null

fun pathToString(path: Path): String = path.joinToString("/")
fun stringToPath(pathStr: String): Path = pathStr.split('/')
fun pathToQualifiedName(path: Path): String = path.joinToString(".")

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

suspend fun getDecompilers(): DecompilersResponse = cachedDecompilers.let { cached ->
    if (cached == null) {
        val decompilers = API.Decompiler.getDecompilersList()
        cachedDecompilers = decompilers
        decompilers
    } else {
        cached
    }
}

fun Select.onSelectValue(handler: (String) -> Unit) {
    onChange { handler(it.asDynamic().target.value) }
}