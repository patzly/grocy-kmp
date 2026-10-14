package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerConnectionCodecTest {

    @Test
    fun apiKeyConnectionSurvivesRoundTrip() {
        val connection = ServerConnection.ApiKey("https://grocy.example.com", "secret")

        val encoded = ServerConnectionCodec.encode(connection)

        assertEquals(connection, ServerConnectionCodec.decode(encoded))
    }

    @Test
    fun demoConnectionSurvivesRoundTrip() {
        val connection = ServerConnection.Demo()

        val encoded = ServerConnectionCodec.encode(connection)

        assertEquals(connection, ServerConnectionCodec.decode(encoded))
    }

    @Test
    fun readsDataWrittenByEarlierVersions() {
        // Stored on devices, so this format must stay readable
        assertEquals(
            ServerConnection.ApiKey("https://grocy.example.com", "secret"),
            ServerConnectionCodec.decode(
                """{"type":"api_key","serverUrl":"https://grocy.example.com","apiKey":"secret"}""",
            ),
        )
        assertEquals(ServerConnection.Demo(), ServerConnectionCodec.decode("""{"type":"demo"}"""))
    }

    @Test
    fun ignoresFieldsAddedByLaterVersions() {
        assertEquals(
            ServerConnection.Demo(),
            ServerConnectionCodec.decode("""{"type":"demo","addedLater":true}"""),
        )
    }

    @Test
    fun unknownConnectionTypeIsNull() {
        assertNull(ServerConnectionCodec.decode("""{"type":"oauth","serverUrl":"https://g"}"""))
    }

    @Test
    fun missingFieldIsNull() {
        assertNull(ServerConnectionCodec.decode("""{"type":"api_key","serverUrl":"https://g"}"""))
    }

    @Test
    fun malformedDataIsNull() {
        assertNull(ServerConnectionCodec.decode(""))
        assertNull(ServerConnectionCodec.decode("not json"))
        assertNull(ServerConnectionCodec.decode("""{"type":"api_key""""))
    }
}
