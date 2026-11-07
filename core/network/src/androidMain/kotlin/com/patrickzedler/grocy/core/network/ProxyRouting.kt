package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Sends OkHttp's connections where [proxy] says, read once: it stays the same for the process,
 * so no pooled connection ever went another way. Never falls back to a direct connection: OkHttp
 * only tries the proxies a selector returns, and a blocked proxy fails before anything connects.
 */
internal class ProxyRouting(
    proxy: suspend () -> NetworkProxy,
    private val system: ProxySelector? = ProxySelector.getDefault(),
    private val torAddress: InetSocketAddress = TorAddress,
) : ProxySelector(), Interceptor {

    // OkHttp asks on its own threads, which may wait for the settings at app start
    private val proxy: NetworkProxy by lazy { runBlocking { proxy() } }

    override fun select(uri: URI): List<Proxy> = when (val current = proxy) {
        NetworkProxy.System -> system?.select(uri) ?: listOf(Proxy.NO_PROXY)
        // An unresolved target, so Tor resolves the name and no DNS query leaves the device
        NetworkProxy.Tor -> listOf(Proxy(Proxy.Type.SOCKS, torAddress))
        is NetworkProxy.Http -> listOf(
            Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(current.host, current.port)),
        )
        // Never reached past the interceptor; a closed port of the device if it were
        NetworkProxy.Blocked -> listOf(Proxy(Proxy.Type.HTTP, BlockedAddress))
    }

    override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
        if (proxy == NetworkProxy.System) system?.connectFailed(uri, address, error)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (proxy == NetworkProxy.Blocked) throw ProxyBlockedException()
        return chain.proceed(chain.request())
    }
}

/** An HTTP proxy is on without a valid host, so nothing may leave the device. */
internal class ProxyBlockedException : IOException("HTTP proxy enabled without a valid host")

private val Loopback = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
private val TorAddress = InetSocketAddress(Loopback, NetworkProxy.Tor.PORT)
private val BlockedAddress = InetSocketAddress(Loopback, 1)
