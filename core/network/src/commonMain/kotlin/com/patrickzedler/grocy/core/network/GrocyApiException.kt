package com.patrickzedler.grocy.core.network

/** Errors the UI can tell apart and explain to the user. */
sealed class GrocyApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** No connection, DNS failure, timeout, or blocked by the browser (mixed content, CORS). */
    class Unreachable(cause: Throwable) : GrocyApiException("Server is not reachable", cause)

    /** HTTP 401: the API key is missing or invalid. */
    class Unauthorized : GrocyApiException("API key was rejected")

    /** The server answered, but not like Grocy does. */
    class NotGrocyServer(cause: Throwable? = null) :
        GrocyApiException("Response is not from a Grocy server", cause)

    class HttpError(val statusCode: Int) : GrocyApiException("Unexpected HTTP status $statusCode")
}
