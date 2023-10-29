package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class EntitiesListFileLoadDelegate<T, V>(
    val fileProperty: KProperty1<T, SequentialFile?>,
    val completeEntitiesProperty: KMutableProperty1<T, List<V>?>,
    val pointersEntitiesProperty: KMutableProperty1<T, List<EntityPointer<V>>?>
) {
    operator fun getValue(thisRef: T, property: KProperty<*>): List<V> {
        val completeEntities = completeEntitiesProperty.get(thisRef)
        if (completeEntities != null) return completeEntities
        else {
            synchronized(thisRef as Any) {
                val completeEntitiesSecond = completeEntitiesProperty.get(thisRef)
                if (completeEntitiesSecond != null) return completeEntitiesSecond
                else {
                    val file = fileProperty.get(thisRef)
                    val entitiesPointers = pointersEntitiesProperty.get(thisRef)

                    if (file == null || entitiesPointers == null) error("Not enough data to complete field data")

                    val loadedEntities = entitiesPointers.map { file.get(it) }
                    completeEntitiesProperty.set(thisRef, loadedEntities)
                    pointersEntitiesProperty.set(thisRef, null)

                    return  loadedEntities
                }
            }
        }
    }
}