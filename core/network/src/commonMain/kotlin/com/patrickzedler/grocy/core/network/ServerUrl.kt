package com.patrickzedler.grocy.core.network

import io.ktor.http.Url

object ServerUrl {

    enum class Scheme(val prefix: String) {
        Https("https://"),
        Http("http://"),
    }

    private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    /**
     * Turns user input into the base URL Grocy expects, e.g.
     * " grocy.example.com/api/ " -> "https://grocy.example.com".
     *
     * An explicit scheme in the input wins over [scheme]. Returns null if the input is no
     * usable URL.
     */
    fun normalize(input: String, scheme: Scheme = Scheme.Https): String? {
        val trimmed = input.trim()
        // Whitespace is never valid inside a URL, requests would fail in confusing ways
        if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return null

        val typedScheme = SCHEME_REGEX.find(trimmed)?.value?.lowercase()
        val prefix = when (typedScheme) {
            null -> scheme.prefix
            Scheme.Https.prefix, Scheme.Http.prefix -> typedScheme
            else -> return null
        }

        var address = trimmed.substring(typedScheme?.length ?: 0).trimEnd('/')
        if (address.endsWith("/api")) {
            address = address.removeSuffix("/api").trimEnd('/')
        }

        // Checked on the input, because Ktor falls back to "localhost" when the host is missing
        val host = address
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
        if (host.isEmpty() || host.startsWith(':')) return null

        val url = prefix + address
        return url.takeIf { runCatching { Url(it) }.isSuccess }
    }
}
