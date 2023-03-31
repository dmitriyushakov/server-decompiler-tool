package com.github.dmitriyushakov.srv_decompiler.decompilers

import com.github.dmitriyushakov.srv_decompiler.decompilers.cfr.CFRDecompiler
import com.github.dmitriyushakov.srv_decompiler.decompilers.jdcore.JDCoreDecompiler
import com.github.dmitriyushakov.srv_decompiler.decompilers.procyon.ProcyonDecompiler

val decompilersList = listOf(
    DecompilerItem("jd_core", "JD Core", JDCoreDecompiler),
    DecompilerItem("cfr", "CFR", CFRDecompiler),
    DecompilerItem("procyon", "Procyon Decompiler", ProcyonDecompiler)
)

val decompilers = decompilersList.associate { it.name to it.decompiler }