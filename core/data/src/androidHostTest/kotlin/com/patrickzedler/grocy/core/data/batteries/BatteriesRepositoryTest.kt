package com.patrickzedler.grocy.core.data.batteries

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BatteriesRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val repository = BatteriesRepository(
        server.api,
        database,
        { ServerConnection.ApiKey("https://grocy.test", "k") },
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresWhatTheServerComputedAndTheBatteries() = runTest {
        repository.refresh()

        val current = repository.current.first().associateBy { it.batteryId }
        assertEquals("2026-10-31 08:00:00", current.getValue(1).nextEstimatedChargeTime)
        assertEquals("2026-10-01 08:00:00", current.getValue(1).lastTrackedTime)
        // Grocy's date for batteries without an interval means there is no next charge cycle
        assertNull(current.getValue(2).nextEstimatedChargeTime)
        assertEquals(2, database.masterDataDao().observeBatteries().first().size)
    }

    @Test
    fun aRefreshReplacesWhatIsNoLongerOnTheServer() = runTest {
        repository.refresh()
        server.respond("/api/batteries", body = "[]")

        repository.refresh()

        assertTrue(repository.current.first().isEmpty())
    }

    @Test
    fun trackingSendsTheTimeAndDownloadsWhatTheServerComputed() = runTest {
        val id = repository.track(1, "2026-11-02 08:00:00")

        assertEquals(23, id)
        val body = server.requestsTo("/api/batteries/1/charge", HttpMethod.Post).single().body
        assertEquals(
            "2026-11-02 08:00:00",
            Json.parseToJsonElement((body as TextContent).text).jsonObject
                .getValue("tracked_time").jsonPrimitive.content,
        )
        assertEquals(2, repository.current.first().size)
        assertEquals(4, repository.details(1).first()?.chargeCyclesCount)
        assertEquals(listOf(23), repository.chargeCycles(null).first().map { it.id })
    }

    @Test
    fun trackingSendsOnlyChangedUserfields() = runTest {
        repository.track(1, "2026-11-02 08:00:00", userfields = mapOf("voltage" to "1.4"))

        assertEquals(
            1,
            server.requestsTo("/api/userfields/battery_charge_cycles/23", HttpMethod.Put).size,
        )
    }

    @Test
    fun aFailedDownloadAfterTrackingDoesNotFailIt() = runTest {
        server.respond("/api/batteries", status = HttpStatusCode.InternalServerError)

        assertEquals(23, repository.track(1, "2026-11-02 08:00:00"))
    }

    @Test
    fun aRejectedTrackingFails() = runTest {
        server.respond(
            "/api/batteries/1/charge",
            status = HttpStatusCode.BadRequest,
            method = HttpMethod.Post,
        )

        assertFailsWith<GrocyApiException> { repository.track(1, "2026-11-02 08:00:00") }
    }

    @Test
    fun undoingStoresTheCycleAsTheServerAnswers() = runTest {
        server.respond(
            "/api/objects/battery_charge_cycles/23",
            body = """{"id":23,"battery_id":1,"tracked_time":"2026-11-02 08:00:00",""" +
                """"undone":1,"undone_timestamp":"2026-11-02 09:00:00"}""",
        )
        repository.track(1, "2026-11-02 08:00:00")

        repository.undo(23, 1)

        val cycle = repository.chargeCycles(null).first().single()
        assertTrue(cycle.undone)
        assertEquals("2026-11-02 09:00:00", cycle.undoneTimestamp)
    }

    @Test
    fun theJournalAsksForItsRangeAndKeepsOlderCycles() = runTest {
        repository.refreshChargeCycles(null)
        server.respond("/api/objects/battery_charge_cycles", body = "[]")

        repository.refreshChargeCycles("2026-09-15")

        val request = server.requestsTo("/api/objects/battery_charge_cycles", HttpMethod.Get).last()
        assertTrue(
            "tracked_time>=2026-09-15" in request.url.parameters.getAll("query[]").orEmpty(),
        )
        // The one in the range was replaced, the older one stays
        assertEquals(listOf(21), repository.chargeCycles(null).first().map { it.id })
        assertTrue(repository.chargeCycles("2026-09-15").first().isEmpty())
    }

    @Test
    fun printingAsksTheServer() = runTest {
        server.respond("/api/batteries/1/printlabel", body = "{}")

        repository.printLabel(1)

        assertEquals(1, server.requestsTo("/api/batteries/1/printlabel", HttpMethod.Get).size)
    }

    @Test
    fun withoutALoginNothingIsSent() = runTest {
        val loggedOut = BatteriesRepository(server.api, database, { null })

        assertFailsWith<GrocyApiException.Unauthorized> { loggedOut.refresh() }
    }
}
