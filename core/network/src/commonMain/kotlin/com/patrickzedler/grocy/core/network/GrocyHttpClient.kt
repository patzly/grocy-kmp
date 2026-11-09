package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val GrocyJson = Json {
    // Grocy adds fields over time, unknown ones must not break older clients
    ignoreUnknownKeys = true
}

/**
 * One client per process, shared by all requests. Every request waits for [proxy] and goes
 * where it says, on Android; browsers use their own proxy settings. Requests into the local
 * network wait at [localNetwork] while the user is asked to allow them (Android only).
 */
fun createGrocyHttpClient(
    localNetwork: LocalNetworkGate? = null,
    tls: TlsOptions? = null,
    proxy: suspend () -> NetworkProxy = { NetworkProxy.System },
): HttpClient = platformHttpClient(proxy, localNetwork, tls) { configureGrocyClient(proxy) }

/**
 * Same configuration as [createGrocyHttpClient] on a given engine, e.g. a mock in tests;
 * [isLocal] stands in for the platform's check of a request's destination.
 */
fun createGrocyHttpClient(
    engine: HttpClientEngine,
    localNetwork: LocalNetworkGate? = null,
    isLocal: suspend (Url) -> Boolean = { false },
    proxy: suspend () -> NetworkProxy = { NetworkProxy.System },
): HttpClient = HttpClient(engine) {
    configureGrocyClient(proxy)
    localNetwork?.let { install(localNetworkPlugin(it, isLocal)) }
}

internal fun HttpClientConfig<*>.configureGrocyClient(proxy: suspend () -> NetworkProxy) {
    install(ContentNegotiation) {
        json(GrocyJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
    }
    install(torTimeouts(proxy))
    // Home Assistant ingress sessions are created through its WebSocket API
    install(WebSockets)
}

/** Tor builds a circuit to a server before the first byte, which takes longer than a direct way. */
private fun torTimeouts(proxy: suspend () -> NetworkProxy) = createClientPlugin("TorTimeouts") {
    onRequest { request, _ ->
        if (proxy() == NetworkProxy.Tor) {
            request.timeout {
                connectTimeoutMillis = TOR_CONNECT_TIMEOUT_MILLIS
                requestTimeoutMillis = TOR_REQUEST_TIMEOUT_MILLIS
            }
        }
    }
}

internal const val TOR_CONNECT_TIMEOUT_MILLIS = 60_000L
internal const val TOR_REQUEST_TIMEOUT_MILLIS = 120_000L
