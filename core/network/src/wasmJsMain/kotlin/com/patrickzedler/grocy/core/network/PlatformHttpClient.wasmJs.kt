package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

// fetch() always goes through the browser's proxy settings, a page cannot choose another
internal actual fun platformHttpClient(
    proxy: suspend () -> NetworkProxy,
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Js, config)
