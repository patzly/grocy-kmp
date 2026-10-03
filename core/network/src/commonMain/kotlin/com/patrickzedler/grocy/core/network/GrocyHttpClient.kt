package com.patrickzedler.grocy.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val GrocyJson = Json {
    // Grocy adds fields over time, unknown ones must not break older clients
    ignoreUnknownKeys = true
}

/** One client per process, shared by all requests. */
fun createGrocyHttpClient(): HttpClient = platformHttpClient {
    install(ContentNegotiation) {
        json(GrocyJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
    }
}
