package com.github.dmitriyushakov.srv_decompiler.decompilers

import com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore.JDCoreDecompiler

val decompilersList = listOf<DecompilerItem>(
    DecompilerItem("jd_core", "JD Core", JDCoreDecompiler)
)

val decompilers = decompilersList.associate { it.name to it.decompiler }