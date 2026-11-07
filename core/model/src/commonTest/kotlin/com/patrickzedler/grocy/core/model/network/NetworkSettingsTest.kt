package com.patrickzedler.grocy.core.model.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkSettingsTest {

    @Test
    fun missingValuesFallBackToDefaults() {
        assertEquals(NetworkSettings(), NetworkSettings.of(null, null, null, null))
    }

    @Test
    fun storedValuesAreKept() {
        assertEquals(
            NetworkSettings(
                useTor = true,
                httpProxyEnabled = true,
                httpProxyHost = "proxy.lan",
                httpProxyPort = 3128,
            ),
            NetworkSettings.of(true, true, "proxy.lan", 3128),
        )
    }

    @Test
    fun invalidStoredHostAndPortFallBackToDefaults() {
        val settings = NetworkSettings.of(null, true, "http://proxy.lan", 70000)

        assertEquals("", settings.httpProxyHost)
        assertEquals(NetworkSettings.DEFAULT_HTTP_PROXY_PORT, settings.httpProxyPort)
    }

    @Test
    fun withoutProxyRequestsGoAsTheSystemSays() {
        assertEquals(NetworkProxy.System, networkProxyOf(NetworkSettings()))
    }

    @Test
    fun torWinsOverTheHttpProxy() {
        val settings = NetworkSettings(
            useTor = true,
            httpProxyEnabled = true,
            httpProxyHost = "proxy.lan",
        )

        assertEquals(NetworkProxy.Tor, networkProxyOf(settings))
    }

    @Test
    fun enabledHttpProxyCarriesItsHostAndPort() {
        val settings = NetworkSettings(
            httpProxyEnabled = true,
            httpProxyHost = "10.0.0.2",
            httpProxyPort = 3128,
        )

        assertEquals(
            NetworkProxy.Http("10.0.0.2", 3128),
            networkProxyOf(settings),
        )
    }

    @Test
    fun enabledHttpProxyWithoutHostBlocksEveryRequest() {
        val settings = NetworkSettings(httpProxyEnabled = true)

        assertEquals(NetworkProxy.Blocked, networkProxyOf(settings))
    }

    @Test
    fun hostsAreNamesOrAddresses() {
        listOf(
            "proxy",
            "proxy.lan",
            "my-proxy.example.com",
            "192.168.1.10",
            "::1",
            "fe80::1",
            "2001:db8:0:0:0:0:0:1",
        ).forEach { assertTrue(isValidProxyHost(it), it) }
    }

    @Test
    fun hostsRejectSchemesPathsPortsAndCredentials() {
        listOf(
            "",
            " ",
            "http://proxy",
            "proxy:8080",
            "proxy/path",
            "user@proxy",
            "-proxy",
            "proxy-.lan",
            "proxy..lan",
            "[::1]",
            "1::2::3",
            "1:2:3:4:5:6:7",
            "12345::1",
            "a".repeat(254),
        ).forEach { assertFalse(isValidProxyHost(it), it) }
    }

    @Test
    fun portsRangeFromOneTo65535() {
        assertFalse(isValidProxyPort(0))
        assertTrue(isValidProxyPort(1))
        assertTrue(isValidProxyPort(MAX_PORT))
        assertFalse(isValidProxyPort(MAX_PORT + 1))
    }
}
