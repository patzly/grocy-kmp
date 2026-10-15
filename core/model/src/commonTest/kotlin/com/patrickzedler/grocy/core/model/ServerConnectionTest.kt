package com.patrickzedler.grocy.core.model

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ServerConnectionTest {

    @Test
    fun apiKeyIsMaskedInToString() {
        val text = ServerConnection.ApiKey("https://grocy.example.com", "top-secret-key").toString()

        assertFalse("top-secret-key" in text)
        assertContains(text, "https://grocy.example.com")
    }

    @Test
    fun homeAssistantSecretsAreMaskedInToString() {
        val text = ServerConnection.HomeAssistant(
            serverUrl = "https://ha.example.com",
            accessToken = "secret-token",
            apiKey = "secret-key",
        ).toString()

        assertFalse("secret-token" in text)
        assertFalse("secret-key" in text)
        assertContains(text, "https://ha.example.com")
    }
}
