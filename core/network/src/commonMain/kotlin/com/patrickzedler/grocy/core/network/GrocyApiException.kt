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

    /**
     * The server rejected the request; [serverMessage] is Grocy's explanation, e.g. a database
     * constraint, shown to the user as technical detail.
     */
    class HttpError(
        val statusCode: Int,
        val serverMessage: String? = null,
    ) : GrocyApiException("Unexpected HTTP status $statusCode")

    /** Home Assistant rejected the access token. */
    class HomeAssistantTokenRejected : GrocyApiException("Access token was rejected")

    /** Home Assistant has no add-on with the slug, or it does not support ingress. */
    class AddonNotFound(cause: Throwable? = null) :
        GrocyApiException("Add-on was not found", cause)

    /** The address answered, but not like Home Assistant does. */
    class NotHomeAssistant(cause: Throwable? = null) :
        GrocyApiException("Response is not from Home Assistant", cause)
}
