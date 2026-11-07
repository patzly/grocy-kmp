package com.patrickzedler.grocy.core.network

import io.ktor.http.Url
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LocalDestinationTest {

    private val resolved = mutableListOf<String>()

    private fun destination(proxy: Proxy = Proxy.NO_PROXY) = LocalDestination(
        select = { listOf(proxy) },
        resolve = { host ->
            resolved += host
            when (host) {
                "grocy" -> address(host, 192, 168, 1, 5)
                "grocy.example.org" -> address(host, 93, 184, 216, 34)
                "proxy.lan" -> address(host, 10, 0, 0, 3)
                // Addresses are no names, the system resolver returns them without asking DNS
                else -> if (host.first().isDigit() || ':' in host) {
                    InetAddress.getAllByName(host).toList()
                } else {
                    throw UnknownHostException(host)
                }
            }
        },
    )

    private fun address(host: String, vararg bytes: Int) =
        listOf(InetAddress.getByAddress(host, ByteArray(bytes.size) { bytes[it].toByte() }))

    @Test
    fun serverWhoseNameResolvesIntoTheHomeNetworkIsLocal() = runTest {
        assertTrue(destination().isLocal(Url("http://grocy/")))
        assertFalse(destination().isLocal(Url("https://grocy.example.org/")))
    }

    @Test
    fun addressesAndMdnsNamesAreLocalWithoutLookup() = runTest {
        assertTrue(destination().isLocal(Url("http://192.168.178.20:9283/")))
        assertTrue(destination().isLocal(Url("http://[fd00::1]/")))
        assertTrue(destination().isLocal(Url("http://grocy.local/")))
        assertFalse(resolved.contains("grocy.local"))
    }

    @Test
    fun unknownNameIsNotLocal() = runTest {
        assertFalse(destination().isLocal(Url("http://nothing.invalid/")))
    }

    @Test
    fun httpProxyInTheHomeNetworkIsTheFirstHop() = runTest {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("proxy.lan", 3128))

        assertTrue(destination(proxy).isLocal(Url("https://grocy.example.org/")))
        // The server's name stays with the proxy
        assertEquals(listOf("proxy.lan"), resolved)
    }

    @Test
    fun torOnTheDeviceIsNotLocalAndResolvesNothing() = runTest {
        val tor = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(InetAddress.getLoopbackAddress(), 9050),
        )

        assertFalse(destination(tor).isLocal(Url("http://grocy/")))
        assertTrue(resolved.isEmpty())
    }
}
