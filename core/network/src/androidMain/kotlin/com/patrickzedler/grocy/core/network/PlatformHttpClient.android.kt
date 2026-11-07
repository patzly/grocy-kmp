package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun platformHttpClient(
    proxy: suspend () -> NetworkProxy,
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient {
    val routing = ProxyRouting(proxy)
    return HttpClient(OkHttp) {
        engine {
            config {
                proxySelector(routing)
                // Also WebSockets pass it, Home Assistant's included
                addInterceptor(routing)
            }
        }
        config()
    }
}
