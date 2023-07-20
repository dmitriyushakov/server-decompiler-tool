package com.github.dmitriyushakov.srv_decompiler.statuses

class BadRequestException: StatusException {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)

    override val status: Int get() = 400
    override val statusText: String get() = "Bad Request"
}

fun badRequest(): Nothing = throw BadRequestException()
fun badRequest(message: String): Nothing = throw BadRequestException(message)
fun badRequest(message: String, cause: Throwable): Nothing = throw BadRequestException(message, cause)
fun badRequest(cause: Throwable): Nothing = throw BadRequestException(cause)