package com.patrickzedler.grocy.core.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.Url
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Lets a request into the local network wait while the user is asked whether the app may reach
 * it (Android 17's `ACCESS_LOCAL_NETWORK`), so a self-hosted server answers right after "Allow"
 * instead of the request timing out. It never stops a request itself: the system decides, so a
 * wrong guess of what is local costs a question at most, never a working connection.
 *
 * Asks once per process and only while a screen can ask ([setCanAsk]); requests without one,
 * e.g. of the notifications at night, go on at once.
 */
class LocalNetworkGate(private val isGranted: () -> Boolean) {

    private val canAsk = MutableStateFlow(false)
    private val mutableIsAsking = MutableStateFlow(false)

    /** True while a request waits for the user's answer. */
    val isAsking: StateFlow<Boolean> = mutableIsAsking.asStateFlow()

    @Volatile
    private var answered = false

    /** Whether a screen is in front that can ask the user; leaving it lets waiting requests go. */
    fun setCanAsk(canAsk: Boolean) {
        this.canAsk.value = canAsk
    }

    /** The user answered, also by declining: no more questions until the app starts again. */
    fun onAnswered() {
        answered = true
        mutableIsAsking.value = false
    }

    /** Waits for the answer before a request whose destination [isLocal] says is local. */
    suspend fun awaitAccess(isLocal: suspend () -> Boolean) {
        if (answered || !canAsk.value || isGranted() || !isLocal()) return
        mutableIsAsking.value = true
        combine(mutableIsAsking, canAsk) { asking, can -> !asking || !can }.first { it }
    }
}

/**
 * Holds every request at [gate] until it may go on; [isLocal] tells whether the first hop of a
 * request to the URL is in the local network.
 */
internal fun localNetworkPlugin(gate: LocalNetworkGate, isLocal: suspend (Url) -> Boolean) =
    createClientPlugin("LocalNetworkGate") {
        // Before the timeouts start, so the time the user takes to answer does not count
        onRequest { request, _ ->
            val url = request.url.build()
            gate.awaitAccess { isLocal(url) }
        }
    }

/**
 * Whether [address] (4 or 16 bytes) is in the local network as Android 17 counts it: private
 * IPv4 ranges, link-local and multicast addresses, IPv6 unique local addresses. Loopback is
 * not, neither is the carrier-grade NAT range VPNs like Tailscale use.
 */
fun isLocalNetworkAddress(address: ByteArray): Boolean {
    val bytes = address.toUnsigned()
    return when (bytes.size) {
        IPV4_SIZE -> isLocalIpv4(bytes)
        IPV6_SIZE -> when {
            // An IPv4 address in IPv6 form (::ffff:a.b.c.d)
            bytes.take(10).all { it == 0 } && bytes[10] == 0xFF && bytes[11] == 0xFF ->
                isLocalIpv4(bytes.drop(12))
            // Unique local fc00::/7, link-local fe80::/10, site-local fec0::/10, multicast ff00::/8
            bytes[0] and 0xFE == 0xFC -> true
            bytes[0] == 0xFE && bytes[1] and 0x80 == 0x80 -> true
            bytes[0] == 0xFF -> true
            else -> false
        }
        else -> false
    }
}

/** Names of the local network's own DNS (mDNS), which only answers with the permission. */
fun isLocalNetworkName(host: String): Boolean =
    host.trimEnd('.').lowercase().let { it == "local" || it.endsWith(".local") }

private fun isLocalIpv4(bytes: List<Int>): Boolean = when {
    bytes[0] == 10 -> true
    bytes[0] == 172 && bytes[1] in 16..31 -> true
    bytes[0] == 192 && bytes[1] == 168 -> true
    bytes[0] == 169 && bytes[1] == 254 -> true
    // Multicast and the broadcast of the network
    bytes[0] in 224..239 -> true
    bytes.all { it == 255 } -> true
    else -> false
}

private fun ByteArray.toUnsigned(): List<Int> = map { it.toInt() and 0xFF }

private const val IPV4_SIZE = 4
private const val IPV6_SIZE = 16
