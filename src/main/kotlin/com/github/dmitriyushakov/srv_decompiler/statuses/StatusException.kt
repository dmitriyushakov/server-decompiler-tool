package com.github.dmitriyushakov.srv_decompiler.statuses

import com.github.dmitriyushakov.srv_decompiler.statuses.responses.ErrorResponse

abstract class StatusException: Exception {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)

    abstract val status: Int
    abstract val statusText: String

    fun getErrorResponse(): ErrorResponse {
        return ErrorResponse(status, statusText, message, cause?.stackTraceToString())
    }
}