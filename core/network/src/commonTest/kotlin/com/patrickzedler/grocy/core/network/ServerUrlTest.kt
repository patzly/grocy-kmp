package com.patrickzedler.grocy.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerUrlTest {

    @Test
    fun addsHttpsWhenSchemeIsMissing() {
        assertEquals("https://grocy.example.com", ServerUrl.normalize("grocy.example.com"))
    }

    @Test
    fun usesSelectedSchemeWhenSchemeIsMissing() {
        assertEquals(
            "http://192.168.1.10:9283",
            ServerUrl.normalize("192.168.1.10:9283", ServerUrl.Scheme.Http),
        )
    }

    @Test
    fun explicitSchemeWinsOverSelectedScheme() {
        assertEquals(
            "https://grocy.example.com",
            ServerUrl.normalize("https://grocy.example.com", ServerUrl.Scheme.Http),
        )
        assertEquals(
            "http://grocy.example.com",
            ServerUrl.normalize("http://grocy.example.com", ServerUrl.Scheme.Https),
        )
    }

    @Test
    fun lowercasesTypedScheme() {
        assertEquals("https://grocy.example.com", ServerUrl.normalize("HTTPS://grocy.example.com"))
    }

    @Test
    fun trimsWhitespaceTrailingSlashesAndApiSuffix() {
        assertEquals("https://grocy.example.com", ServerUrl.normalize(" grocy.example.com/api/ "))
        assertEquals("https://grocy.example.com", ServerUrl.normalize("grocy.example.com//"))
        assertEquals("https://grocy.example.com", ServerUrl.normalize("grocy.example.com/api"))
    }

    @Test
    fun keepsSubpathOfReverseProxyInstallations() {
        assertEquals(
            "https://example.com/grocy",
            ServerUrl.normalize("https://example.com/grocy/api/"),
        )
    }

    @Test
    fun keepsPathsThatOnlyContainApi() {
        assertEquals("https://example.com/myapi", ServerUrl.normalize("example.com/myapi"))
    }

    @Test
    fun acceptsLocalHostnamesAndIpv6() {
        assertEquals("https://localhost:9283", ServerUrl.normalize("localhost:9283"))
        assertEquals("http://[::1]:9283", ServerUrl.normalize("http://[::1]:9283"))
    }

    @Test
    fun rejectsBlankInput() {
        assertNull(ServerUrl.normalize(""))
        assertNull(ServerUrl.normalize("   "))
        assertNull(ServerUrl.normalize("/api/"))
    }

    @Test
    fun rejectsSchemeWithoutHost() {
        assertNull(ServerUrl.normalize("https://"))
        assertNull(ServerUrl.normalize("http:///api"))
        assertNull(ServerUrl.normalize("https://:9283"))
        assertNull(ServerUrl.normalize("https://user@/grocy"))
    }

    @Test
    fun rejectsOtherProtocols() {
        assertNull(ServerUrl.normalize("ftp://grocy.example.com"))
        assertNull(ServerUrl.normalize("file:///etc/passwd"))
        assertNull(ServerUrl.normalize("javascript://alert(1)"))
    }

    @Test
    fun rejectsWhitespaceInsideTheAddress() {
        assertNull(ServerUrl.normalize("grocy example.com"))
        assertNull(ServerUrl.normalize("https://grocy.example.com/my path"))
    }
}
