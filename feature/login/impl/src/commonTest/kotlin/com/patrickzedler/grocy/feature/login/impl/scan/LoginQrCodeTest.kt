package com.patrickzedler.grocy.feature.login.impl.scan

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginQrCodeTest {

    private val key = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718"

    private fun apiKey(serverUrl: String) =
        LoginQrCode.ApiKey(ServerConnection.ApiKey(serverUrl = serverUrl, apiKey = key))

    @Test
    fun readsCodeOfApiKeyPage() {
        assertEquals(
            apiKey("https://grocy.example.com"),
            LoginQrCode.parse("https://grocy.example.com/api|$key"),
        )
    }

    @Test
    fun keepsPortAndSubpath() {
        assertEquals(
            apiKey("http://192.168.1.10:9283/grocy"),
            LoginQrCode.parse("http://192.168.1.10:9283/grocy/api|$key"),
        )
    }

    @Test
    fun keepsIndexPhpOfServersWithoutUrlRewriting() {
        assertEquals(
            apiKey("https://example.com/index.php"),
            LoginQrCode.parse("https://example.com/index.php/api|$key"),
        )
    }

    @Test
    fun ignoresSurroundingWhitespaceAndTrailingSlash() {
        assertEquals(
            apiKey("https://grocy.example.com"),
            LoginQrCode.parse("  https://grocy.example.com/api/|$key\n"),
        )
    }

    @Test
    fun calendarLinkIsRecognized() {
        assertEquals(
            LoginQrCode.CalendarLink,
            LoginQrCode.parse("https://grocy.example.com/api/calendar/ical?secret=$key"),
        )
    }

    @Test
    fun otherCodesAreInvalid() {
        listOf(
            "4006381333931",
            "grcy:p:12",
            "https://example.com",
            "https://grocy.example.com/api",
            "https://grocy.example.com/api|",
            "https://grocy.example.com/other|$key",
            "/grocy/api|$key",
            "ftp://grocy.example.com/api|$key",
            "https://grocy.example.com/api|two words",
        ).forEach {
            assertEquals(LoginQrCode.Invalid, LoginQrCode.parse(it), it)
        }
    }
}
