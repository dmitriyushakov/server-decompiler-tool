package com.github.dmitriyushakov.srv_decompiler.indexer.persisted

import com.github.dmitriyushakov.srv_decompiler.common.seqfile.EntityPointer
import com.github.dmitriyushakov.srv_decompiler.common.seqfile.SequentialFile
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class EntityFileLoadDelegate<T, V>(
    val fileProperty: KProperty1<T, SequentialFile?>,
    val completeEntityProperty: KMutableProperty1<T, V?>,
    val entityPointerProperty: KMutableProperty1<T, EntityPointer<V>?>
) {
    operator fun getValue(thisRef: T, property: KProperty<*>): V {
        val completeEntity = completeEntityProperty.get(thisRef)
        if (completeEntity != null) return completeEntity
        else {
            synchronized(thisRef as Any) {
                val completeEntitySecond = completeEntityProperty.get(thisRef)
                if (completeEntitySecond != null) return completeEntitySecond
                else {
                    val file = fileProperty.get(thisRef)
                    val entityPointer = entityPointerProperty.get(thisRef)

                    if (file == null || entityPointer == null) error("Not enough data to complete field data")

                    val loadedEntity = file.get(entityPointer)
                    completeEntityProperty.set(thisRef, loadedEntity)
                    entityPointerProperty.set(thisRef, null)

                    return  loadedEntity
                }
            }
        }
    }
}