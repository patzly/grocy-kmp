package com.patrickzedler.grocy.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/** Creates an HttpClient with the engine of the current platform. */
internal expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
