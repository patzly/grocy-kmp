package com.patrickzedler.grocy.core.network

import io.ktor.http.Url
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Whether a request's first hop is in the local network: the server itself without a proxy,
 * else the proxy [select] chooses. Behind a proxy the server's name is never resolved here, so
 * Tor still gets every name unresolved.
 */
internal class LocalDestination(
    private val select: (URI) -> List<Proxy>,
    // DNS queries to the network's own server need no permission, so resolving is allowed
    private val resolve: (String) -> List<InetAddress> = { InetAddress.getAllByName(it).toList() },
) {
    suspend fun isLocal(url: Url): Boolean = withContext(Dispatchers.IO) {
        val proxy = select(URI(url.toString())).firstOrNull() ?: Proxy.NO_PROXY
        val address = proxy.address() as? InetSocketAddress
        when {
            proxy.type() == Proxy.Type.DIRECT -> isLocalHost(url.host)
            address == null -> false
            address.address != null -> isLocalNetworkAddress(address.address.address)
            else -> isLocalHost(address.hostString)
        }
    }

    private fun isLocalHost(host: String): Boolean {
        val name = host.removePrefix("[").removeSuffix("]")
        if (isLocalNetworkName(name)) return true
        val addresses = try {
            resolve(name)
        } catch (_: UnknownHostException) {
            return false
        } catch (_: SecurityException) {
            return false
        }
        return addresses.any { isLocalNetworkAddress(it.address) }
    }
}
