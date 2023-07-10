package com.github.dmitriyushakov.srv_decompiler.decompilers

data class DecompilersResponse (val decompilers: List<DecompilersListItem>) {
    data class DecompilersListItem(val name: String, val displayName: String)

    companion object {
        fun make(decompilers: List<DecompilerItem>): DecompilersResponse =
            DecompilersResponse(
                decompilers = decompilers.map {
                    DecompilersListItem(
                        name = it.name,
                        displayName = it.displayName
                    )
                }
            )
    }
}