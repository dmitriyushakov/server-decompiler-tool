package com.github.dmitriyushakov.srv_decompiler.decompilers

import com.github.dmitriyushakov.srv_decompiler.decompilers.cfr.CFRDecompiler
import com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore.JDCoreDecompiler

val decompilersList = listOf(
    DecompilerItem("jd_core", "JD Core", JDCoreDecompiler),
    DecompilerItem("cfr", "CFR", CFRDecompiler)
)

val decompilers = decompilersList.associate { it.name to it.decompiler }