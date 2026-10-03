package com.patrickzedler.grocy.core.network

import io.ktor.http.Url

object ServerUrl {

    enum class Scheme(val prefix: String) {
        Https("https://"),
        Http("http://"),
    }

    /**
     * Turns user input into the base URL Grocy expects, e.g.
     * " grocy.example.com/api/ " -> "https://grocy.example.com".
     *
     * An explicit scheme in the input wins over [scheme]. Returns null if the input is no usable URL.
     */
    fun normalize(input: String, scheme: Scheme = Scheme.Https): String? {
        var url = input.trim().trimEnd('/')
        if (url.endsWith("/api")) {
            url = url.removeSuffix("/api").trimEnd('/')
        }
        if ("://" !in url) {
            url = scheme.prefix + url
        }
        val parsed = runCatching { Url(url) }.getOrNull() ?: return null
        val isHttp = parsed.protocol.name == "http" || parsed.protocol.name == "https"
        return url.takeIf { isHttp && parsed.host.isNotBlank() }
    }
}
