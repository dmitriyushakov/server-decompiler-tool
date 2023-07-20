package com.github.dmitriyushakov.srv_decompiler.statuses.responses

data class ErrorResponse (val status: Int, val statusText: String, val message: String?, val cause: String?)