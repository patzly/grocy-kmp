package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GrocyApiCalendarTest {

    private val connection = ServerConnection.ApiKey("https://grocy.example", "key")

    @Test
    fun theSharingLinkComesFromGrocysEndpoint() = runTest {
        var path = ""
        val engine = MockEngine { request ->
            path = request.url.encodedPath
            respond(
                content = """{"url":"https://grocy.example/api/calendar/ical?secret=abc"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val link = GrocyApi(createGrocyHttpClient(engine)).calendarSharingLink(connection)

        assertEquals("/api/calendar/ical/sharing-link", path)
        assertEquals("https://grocy.example/api/calendar/ical?secret=abc", link)
    }
}
