package com.patrickzedler.grocy.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class LocalNetworkGateTest {

    private var granted = false
    private val gate = LocalNetworkGate { granted }

    @Test
    fun localRequestWaitsForTheAnswer() = runTest {
        gate.setCanAsk(true)

        val request = async { gate.awaitAccess { true } }
        runCurrent()

        assertTrue(gate.isAsking.value)
        assertFalse(request.isCompleted)
        gate.onAnswered()
        runCurrent()
        assertTrue(request.isCompleted)
        assertFalse(gate.isAsking.value)
    }

    @Test
    fun answeredOnceAsksNoMore() = runTest {
        gate.setCanAsk(true)
        gate.onAnswered()

        gate.awaitAccess { true }

        assertFalse(gate.isAsking.value)
    }

    @Test
    fun withoutAScreenRequestsGoOnAtOnce() = runTest {
        gate.awaitAccess { true }

        assertFalse(gate.isAsking.value)
    }

    @Test
    fun leavingTheScreenLetsWaitingRequestsGo() = runTest {
        gate.setCanAsk(true)
        val request = async { gate.awaitAccess { true } }
        runCurrent()

        gate.setCanAsk(false)
        runCurrent()

        assertTrue(request.isCompleted)
    }

    @Test
    fun grantedOrNotLocalNeedsNoQuestion() = runTest {
        gate.setCanAsk(true)
        var checkedDestination = false

        gate.awaitAccess { false }
        granted = true
        gate.awaitAccess {
            checkedDestination = true
            true
        }

        assertFalse(gate.isAsking.value)
        // With the permission the destination is not even looked up
        assertFalse(checkedDestination)
    }

    @Test
    fun clientRequestsWaitAtTheGate() = runTest {
        gate.setCanAsk(true)
        var sent = 0
        val engine = MockEngine {
            sent++
            respondOk()
        }
        val client = createGrocyHttpClient(
            engine = engine,
            localNetwork = gate,
            isLocal = { it.host == "192.168.1.5" },
        )

        client.get("https://grocy.example.org/")
        val local = async { client.get("http://192.168.1.5/") }
        runCurrent()

        assertEquals(1, sent)
        assertTrue(gate.isAsking.value)
        gate.onAnswered()
        local.await()
        assertEquals(2, sent)
    }

    @Test
    fun privateAndLinkLocalAddressesAreLocal() {
        listOf(
            byteArrayOf(10, 0, 0, 1),
            ipv4(172, 16, 0, 1),
            ipv4(172, 31, 255, 254),
            ipv4(192, 168, 1, 20),
            ipv4(169, 254, 3, 4),
            ipv4(224, 0, 0, 251),
            ipv4(255, 255, 255, 255),
            ipv6(0xFD, 0x12),
            ipv6(0xFE, 0x80),
            ipv6(0xFF, 0x02),
            mappedIpv4(192, 168, 0, 2),
        ).forEach { assertTrue(isLocalNetworkAddress(it), it.joinToString()) }
    }

    @Test
    fun publicLoopbackAndVpnAddressesAreNotLocal() {
        listOf(
            ipv4(8, 8, 8, 8),
            ipv4(127, 0, 0, 1),
            ipv4(172, 32, 0, 1),
            ipv4(100, 100, 1, 1),
            ipv4(192, 169, 0, 1),
            ipv6(0x20, 0x01),
            ipv6(0, 0).also { it[15] = 1 },
            mappedIpv4(8, 8, 4, 4),
            ByteArray(3),
        ).forEach { assertFalse(isLocalNetworkAddress(it), it.joinToString()) }
    }

    @Test
    fun mdnsNamesAreLocal() {
        assertTrue(isLocalNetworkName("grocy.local"))
        assertTrue(isLocalNetworkName("Grocy.LOCAL."))
        assertFalse(isLocalNetworkName("grocy.localhost.example"))
        assertFalse(isLocalNetworkName("grocy.example.org"))
    }

    private fun ipv4(a: Int, b: Int, c: Int, d: Int) =
        byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())

    private fun ipv6(first: Int, second: Int) =
        ByteArray(16).also {
            it[0] = first.toByte()
            it[1] = second.toByte()
        }

    private fun mappedIpv4(a: Int, b: Int, c: Int, d: Int) =
        ByteArray(16).also {
            it[10] = 0xFF.toByte()
            it[11] = 0xFF.toByte()
            ipv4(a, b, c, d).copyInto(it, destinationOffset = 12)
        }
}
