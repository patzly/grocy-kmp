package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GrocyHttpClientTest {

    @Test
    fun torGetsLongerTimeoutsForBuildingItsCircuit() = runTest {
        val timeouts = connectAndRequestTimeouts(NetworkProxy.Tor)

        assertEquals(TOR_CONNECT_TIMEOUT_MILLIS to TOR_REQUEST_TIMEOUT_MILLIS, timeouts)
    }

    @Test
    fun otherWaysKeepTheUsualTimeouts() = runTest {
        val timeouts = connectAndRequestTimeouts(NetworkProxy.Http("proxy.lan", 8080))

        assertEquals(15_000L to 30_000L, timeouts)
    }

    private suspend fun connectAndRequestTimeouts(proxy: NetworkProxy): Pair<Long?, Long?> {
        var timeouts: Pair<Long?, Long?>? = null
        val engine = MockEngine { request ->
            val capability = request.getCapabilityOrNull(HttpTimeoutCapability)
            timeouts = capability?.connectTimeoutMillis to capability?.requestTimeoutMillis
            respondOk()
        }

        createGrocyHttpClient(engine) { proxy }.get("https://grocy.invalid/")

        return checkNotNull(timeouts)
    }
}
