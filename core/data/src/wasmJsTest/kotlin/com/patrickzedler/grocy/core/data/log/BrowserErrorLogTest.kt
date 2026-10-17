package com.patrickzedler.grocy.core.data.log

import com.patrickzedler.grocy.core.data.InMemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class BrowserErrorLogTest {

    private val storage = InMemoryStorage()

    private fun log() = BrowserErrorLog(storage, timestamp = { "10-17 14:03:59.042" })

    @Test
    fun errorsAreFormattedLikeLogcat() = runTest {
        val log = log()

        log.add("TypeError: boom\n    at main (grocy.js:1)\n")

        assertEquals(
            "10-17 14:03:59.042 E TypeError: boom\n" +
                "10-17 14:03:59.042 E     at main (grocy.js:1)",
            log.readErrors(),
        )
    }

    @Test
    fun apiKeyInUrlsIsRemoved() = runTest {
        val log = log()

        log.add("GET https://grocy.example/api/system/info?GROCY-API-KEY=s3cr3t&x=1 failed")

        val text = log.readErrors()
        assertFalse("s3cr3t" in text)
        assertEquals(true, "?GROCY-API-KEY=***&x=1 failed" in text)
    }

    @Test
    fun tokensAndSessionsAreRemoved() = runTest {
        val log = log()

        log.add("""{"type":"auth","access_token":"ha-token"} Bearer abc.def ingress_session=xyz;""")

        val text = log.readErrors()
        listOf("ha-token", "abc.def", "xyz").forEach { assertFalse(it in text, it) }
        assertEquals(
            """{"type":"auth","access_token":"***"} Bearer *** ingress_session=***;""",
            text.substringAfter(" E "),
        )
    }

    @Test
    fun onlyTheNewestLinesAreKept() = runTest {
        val log = log()

        repeat(BrowserErrorLog.MAX_LINES + 5) { log.add("error $it") }

        val lines = log.readErrors().lines()
        assertEquals(BrowserErrorLog.MAX_LINES, lines.size)
        assertEquals("10-17 14:03:59.042 E error 5", lines.first())
    }

    @Test
    fun errorsSurviveAReload() = runTest {
        log().add("before reload")

        val reloaded = log()

        assertEquals("10-17 14:03:59.042 E before reload", reloaded.readErrors())
    }

    @Test
    fun storedLogIsRedactedToo() = runTest {
        log().add("GROCY-API-KEY: s3cr3t")

        assertFalse("s3cr3t" in storage.values.getValue(BrowserErrorLog.KEY))
    }
}
