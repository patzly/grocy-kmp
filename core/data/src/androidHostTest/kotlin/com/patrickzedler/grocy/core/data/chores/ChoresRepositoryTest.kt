package com.patrickzedler.grocy.core.data.chores

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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChoresRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val repository = ChoresRepository(
        server.api,
        database,
        { ServerConnection.ApiKey("https://grocy.test", "k") },
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun bodyOf(path: String, method: HttpMethod = HttpMethod.Post) =
        server.requestsTo(path, method).map {
            Json.parseToJsonElement((it.body as TextContent).text).jsonObject
        }

    @Test
    fun refreshStoresTheOverviewAndTheUser() = runTest {
        repository.refresh()

        val chores = repository.current.first()
        assertEquals(listOf("Vacuum", "Water plants"), chores.map { it.name })
        val vacuum = chores.first()
        assertEquals("2026-11-02 08:00:00", vacuum.nextEstimatedExecutionTime)
        assertEquals("2026-10-20 08:00:00", vacuum.lastTrackedTime)
        assertEquals(1, vacuum.assignedUserId)
        assertTrue(vacuum.isRescheduled)
        assertFalse(vacuum.isReassigned)
        assertNull(chores.last().nextEstimatedExecutionTime)
        assertTrue(chores.last().trackDateOnly)
        assertEquals(1, repository.currentUserId.first())
    }

    @Test
    fun theDateOlderServersAnswerWithoutScheduleMeansNoSchedule() = runTest {
        server.respond(
            "/api/chores",
            body = """[{"chore_id":1,"chore_name":"Vacuum",""" +
                """"next_estimated_execution_time":"2999-12-31 23:59:59"}]""",
        )

        repository.refresh()

        assertNull(repository.current.first().single().nextEstimatedExecutionTime)
    }

    @Test
    fun aFailedRefreshKeepsWhatIsStored() = runTest {
        repository.refresh()

        server.unreachable = true
        assertFailsWith<GrocyApiException.Unreachable> { repository.refresh() }

        assertEquals(2, repository.current.first().size)
    }

    @Test
    fun detailsComeFromTheServer() = runTest {
        repository.refreshDetails(1)

        val details = repository.details(1).first()!!
        assertEquals(4, details.trackedCount)
        assertEquals("Alice", details.lastDoneByName)
        assertEquals(72.5, details.averageFrequencyHours)
        assertEquals("2026-10-20 08:00:00", details.lastTracked)
        assertNull(repository.details(3).first())
    }

    @Test
    fun journalAsksForItsRangeNewestFirst() = runTest {
        repository.refreshExecutions("2026-01-01")

        val request = server.requestsTo("/api/objects/chores_log", HttpMethod.Get).single()
        assertEquals(
            listOf("tracked_time>=2026-01-01"),
            request.url.parameters.getAll("query[]"),
        )
        val executions = repository.executions("2026-01-01").first()
        assertEquals(listOf(12, 11, 10), executions.map { it.id })
        assertEquals("Good", executions.first().userfields["mood"])
        assertTrue(executions[1].isLate)
        assertTrue(executions[2].skipped)
        assertTrue(executions[2].undone)
        assertEquals("2026-10-02 10:00:00", executions[2].undoneTimestamp)
    }

    @Test
    fun aDownloadReplacesOnlyItsRange() = runTest {
        repository.refreshExecutions(null)
        server.respond("/api/objects/chores_log", body = "[]")

        repository.refreshExecutions("2026-10-15")

        assertEquals(listOf(11, 10), repository.executions(null).first().map { it.id })
    }

    @Test
    fun theJournalShowsARangeOnly() = runTest {
        repository.refreshExecutions(null)

        assertEquals(listOf(12), repository.executions("2026-10-15").first().map { it.id })
    }

    @Test
    fun trackingSendsLikeGrocyAndStoresWhatTheServerComputed() = runTest {
        val id = repository.track(1, "2026-11-02 08:00:00", doneByUserId = 2, skipped = true)

        assertEquals(13, id)
        val body = bodyOf("/api/chores/1/execute").single()
        assertEquals("2026-11-02 08:00:00", body["tracked_time"]!!.jsonPrimitive.content)
        assertEquals(2, body["done_by"]!!.jsonPrimitive.int)
        assertTrue(body["skipped"]!!.jsonPrimitive.boolean)
        // The next execution comes from the server, the journal knows the execution at once
        assertEquals(2, repository.current.first().size)
        assertEquals(4, repository.details(1).first()!!.trackedCount)
        assertEquals(listOf(13), repository.executions(null).first().map { it.id })
    }

    @Test
    fun trackingWithoutUserLeavesItToTheServer() = runTest {
        repository.track(1, "2026-11-02 08:00:00")

        assertFalse("done_by" in bodyOf("/api/chores/1/execute").single())
    }

    @Test
    fun onlyChangedUserfieldsAreSentAfterTracking() = runTest {
        repository.track(1, "2026-11-02 08:00:00")
        assertTrue(server.requestsTo("/api/userfields/chores_log/13", HttpMethod.Put).isEmpty())

        repository.track(1, "2026-11-02 08:00:00", userfields = mapOf("mood" to "Good"))

        val body = bodyOf("/api/userfields/chores_log/13", HttpMethod.Put).single()
        assertEquals("Good", body["mood"]!!.jsonPrimitive.content)
    }

    @Test
    fun aFailedDownloadAfterTrackingDoesNotFailTheTracking() = runTest {
        server.respond("/api/chores", status = HttpStatusCode.InternalServerError)

        assertEquals(13, repository.track(1, "2026-11-02 08:00:00"))
    }

    @Test
    fun trackingOfflineFails() = runTest {
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> {
            repository.track(1, "2026-11-02 08:00:00")
        }
    }

    @Test
    fun undoingStoresTheExecutionAsTheServerHasItNow() = runTest {
        repository.track(1, "2026-11-02 08:00:00")
        server.respond(
            "/api/objects/chores_log/13",
            body = """{"id":13,"chore_id":1,"tracked_time":"2026-11-02 08:00:00","undone":1,""" +
                """"undone_timestamp":"2026-11-02 09:00:00"}""",
        )

        repository.undo(13, 1)

        assertEquals(1, server.requestsTo("/api/chores/executions/13/undo", HttpMethod.Post).size)
        assertTrue(repository.executions(null).first().single().undone)
    }

    @Test
    fun reschedulingSavesTheChoreThenAssignsAgain() = runTest {
        repository.reschedule(1, "2026-12-24 10:00:00", 2)

        val body = bodyOf("/api/objects/chores/1", HttpMethod.Put).single()
        assertEquals("2026-12-24 10:00:00", body["rescheduled_date"]!!.jsonPrimitive.content)
        assertEquals(
            2,
            body["rescheduled_next_execution_assigned_to_user_id"]!!.jsonPrimitive.int,
        )
        val assignment = bodyOf("/api/chores/executions/calculate-next-assignments").single()
        assertEquals(1, assignment["chore_id"]!!.jsonPrimitive.int)
    }

    @Test
    fun resettingForgetsDateAndUser() = runTest {
        repository.reschedule(1, null, null)

        val body = bodyOf("/api/objects/chores/1", HttpMethod.Put).single()
        assertEquals(JsonNull, body["rescheduled_date"])
        assertEquals(JsonNull, body["rescheduled_next_execution_assigned_to_user_id"])
    }

    @Test
    fun labelsPrintOnTheServer() = runTest {
        server.respond("/api/chores/1/printlabel", body = "{}")

        repository.printLabel(1)

        assertEquals(1, server.requestsTo("/api/chores/1/printlabel", HttpMethod.Get).size)
    }

    @Test
    fun theServerMayNotBeReadWithoutLogin() = runTest {
        val loggedOut = ChoresRepository(server.api, database, { null })

        assertFailsWith<GrocyApiException.Unauthorized> { loggedOut.refresh() }
    }
}
