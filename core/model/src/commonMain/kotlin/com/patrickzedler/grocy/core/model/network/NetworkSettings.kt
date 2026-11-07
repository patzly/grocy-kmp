package com.patrickzedler.grocy.core.model.network

/**
 * How the app's requests leave the device, kept on the device (Android only, browsers decide
 * this themselves): through Tor ([useTor], Orbot's SOCKS port), else through the HTTP proxy
 * [httpProxyHost]:[httpProxyPort] while [httpProxyEnabled], else as the system says. A process
 * keeps the settings it started with, so its connections never mix proxies.
 */
data class NetworkSettings(
    val useTor: Boolean = false,
    val httpProxyEnabled: Boolean = false,
    val httpProxyHost: String = "",
    val httpProxyPort: Int = DEFAULT_HTTP_PROXY_PORT,
) {
    companion object {
        const val DEFAULT_HTTP_PROXY_PORT = 8080

        /** Settings from their stored parts; missing or invalid values fall back to defaults. */
        fun of(
            useTor: Boolean?,
            httpProxyEnabled: Boolean?,
            httpProxyHost: String?,
            httpProxyPort: Int?,
        ): NetworkSettings {
            val default = NetworkSettings()
            return NetworkSettings(
                useTor = useTor ?: default.useTor,
                httpProxyEnabled = httpProxyEnabled ?: default.httpProxyEnabled,
                httpProxyHost = httpProxyHost?.takeIf(::isValidProxyHost) ?: default.httpProxyHost,
                httpProxyPort = httpProxyPort?.takeIf(::isValidProxyPort)
                    ?: default.httpProxyPort,
            )
        }
    }
}

/** Where a request goes. */
sealed interface NetworkProxy {

    /** As the system says, its proxy for the network if it has one. */
    data object System : NetworkProxy

    /** Orbot's SOCKS port, which resolves host names in Tor, so no name leaves the device. */
    data object Tor : NetworkProxy {
        const val HOST = "127.0.0.1"
        const val PORT = 9050
    }

    data class Http(val host: String, val port: Int) : NetworkProxy

    /** An HTTP proxy is on without a valid host: nothing may go out without it. */
    data object Blocked : NetworkProxy
}

/**
 * Where requests go with [settings]: Tor before the HTTP proxy, an HTTP proxy without a valid host
 * blocks them.
 */
fun networkProxyOf(settings: NetworkSettings): NetworkProxy = when {
    settings.useTor -> NetworkProxy.Tor
    !settings.httpProxyEnabled -> NetworkProxy.System
    isValidProxyHost(settings.httpProxyHost) && isValidProxyPort(settings.httpProxyPort) ->
        NetworkProxy.Http(settings.httpProxyHost, settings.httpProxyPort)
    else -> NetworkProxy.Blocked
}

/**
 * A proxy's host: a name ("proxy.lan"), IPv4 or IPv6 address, nothing else, so no scheme, path,
 * port or credentials can slip into it.
 */
fun isValidProxyHost(host: String): Boolean =
    host.length <= MAX_HOST_LENGTH && (HostName.matches(host) || isIpv6Address(host))

fun isValidProxyPort(port: Int): Boolean = port in 1..MAX_PORT

const val MAX_PORT = 65535

private fun isIpv6Address(host: String): Boolean {
    if (!Ipv6Chars.matches(host) || host.count { it == ':' } < 2) return false
    val compressions = host.windowed(2).count { it == "::" }
    if (compressions > 1 || ":::" in host) return false
    val groups = host.split(":")
    val filled = groups.filter { it.isNotEmpty() }
    return filled.all { it.length <= 4 } &&
        (if (compressions == 1) filled.size < 8 else groups.size == 8 && filled.size == 8)
}

private const val MAX_HOST_LENGTH = 253

// Labels of letters, digits and hyphens, not starting or ending with a hyphen; IPv4 fits too
private const val HOST_LABEL = "[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
private val HostName = Regex("^$HOST_LABEL(\\.$HOST_LABEL)*$")

private val Ipv6Chars = Regex("^[0-9A-Fa-f:]+$")
