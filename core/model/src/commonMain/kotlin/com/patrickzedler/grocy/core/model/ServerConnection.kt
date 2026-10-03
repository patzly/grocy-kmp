package com.patrickzedler.grocy.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the app talks to a Grocy server. Serializable so the credential store can persist it.
 */
@Serializable
sealed interface ServerConnection {

    /** Base URL of the Grocy instance, without trailing slash and without "/api". */
    val serverUrl: String

    /** Public demo instance, which runs without authentication. */
    @Serializable
    @SerialName("demo")
    data class Demo(
        override val serverUrl: String = DEFAULT_URL,
    ) : ServerConnection {
        companion object {
            const val DEFAULT_URL = "https://en.demo.grocy.info"
        }
    }

    @Serializable
    @SerialName("api_key")
    data class ApiKey(
        override val serverUrl: String,
        val apiKey: String,
    ) : ServerConnection {
        // Keeps the key out of logs and crash reports
        override fun toString(): String = "ApiKey(serverUrl=$serverUrl, apiKey=***)"
    }
}
