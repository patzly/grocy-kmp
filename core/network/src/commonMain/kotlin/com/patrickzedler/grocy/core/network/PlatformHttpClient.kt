package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Creates an HttpClient with the engine of the current platform, its requests through [proxy]
 * where the platform lets the app choose.
 */
internal expect fun platformHttpClient(
    proxy: suspend () -> NetworkProxy,
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient
