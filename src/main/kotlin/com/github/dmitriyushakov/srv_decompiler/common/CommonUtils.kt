package com.github.dmitriyushakov.srv_decompiler.common

import java.util.Optional

fun <T> ref(value: T): SharedMutableReference<T> = SharedMutableReference(value)

fun <T> Optional<T>?.getOrNull(): T? = if (this != null && this.isPresent) this.get() else null