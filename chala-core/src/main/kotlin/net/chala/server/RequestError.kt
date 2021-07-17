package net.chala.server

internal data class JavalinError(val status: Int, val title: String)

data class RequestError(val status: Int, val cause: String?)