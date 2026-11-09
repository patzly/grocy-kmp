package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun platformHttpClient(
    proxy: suspend () -> NetworkProxy,
    localNetwork: LocalNetworkGate?,
    tls: TlsOptions?,
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient {
    val routing = ProxyRouting(proxy)
    return HttpClient(OkHttp) {
        engine {
            config {
                proxySelector(routing)
                // Also WebSockets pass it, Home Assistant's included
                addInterceptor(routing)
                tls?.let { GrocyTls(it).applyTo(this) }
            }
        }
        config()
        if (localNetwork != null) {
            val destination = LocalDestination(routing::select)
            install(localNetworkPlugin(localNetwork, destination::isLocal))
        }
    }
}
