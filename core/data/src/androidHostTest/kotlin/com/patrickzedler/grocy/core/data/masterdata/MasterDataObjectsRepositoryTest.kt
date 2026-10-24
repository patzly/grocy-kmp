package com.patrickzedler.grocy.core.data.masterdata

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** The master data besides products: chores, batteries, userfields and the simple objects. */
class MasterDataObjectsRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val repository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { ServerConnection.ApiKey("https://grocy.test", "k") },
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresEveryKindOfMasterData() = runTest {
        repository.refresh()

        val chore = repository.chores.first().first { it.name == "Vacuum" }
        assertEquals(ChorePeriodType.WEEKLY, chore.periodType)
        assertEquals(setOf(Weekday.MONDAY, Weekday.FRIDAY), chore.weekdays)
        assertEquals(listOf(1), chore.assignedUserIds)
        assertEquals("Living room", chore.userfields["room"])
        assertEquals(30, repository.batteries.first().single().chargeIntervalDays)
        assertEquals("Garden", repository.taskCategories.first().single().name)
        assertEquals("Books", repository.userentities.first().single().caption)
        assertEquals(listOf("Alice", "Bob"), repository.users.first().map { it.displayName })
        val entities = repository.allUserfields.first().map { it.entity }
        assertEquals(listOf("chores", "products"), entities)
        assertEquals("room", repository.userfields("chores").first().single().name)
    }

    @Test
    fun usersStayWhenTheUserMayNotReadThemAnymore() = runTest {
        repository.refresh()
        server.respond("/api/users", HttpStatusCode.Forbidden, """{"error_message":"No"}""")

        repository.refresh()

        assertEquals(2, repository.users.first().size)
        assertEquals("Garden", repository.taskCategories.first().single().name)
    }

    @Test
    fun otherFailuresOfTheUsersFailTheRefresh() = runTest {
        server.respond("/api/users", HttpStatusCode.InternalServerError, "{}")

        assertFailsWith<GrocyApiException.HttpError> { repository.refresh() }
    }

    @Test
    fun newLocationIsCreatedWithItsUserfieldsAndStoredAsSaved() = runTest {
        repository.refresh()
        server.respond(
            "/api/objects/locations",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        server.respond(
            "/api/objects/locations/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"name":"Cellar","is_freezer":1,""" +
                """"userfields":{"shelf":"3"}}""",
        )

        val location = Location(0, "Cellar", isFreezer = true)

        val id = repository.saveLocation(location, mapOf("shelf" to "3"))

        assertEquals(MasterDataJson.CREATED_ID, id)
        val userfieldPath = "/api/userfields/locations/${MasterDataJson.CREATED_ID}"
        assertEquals(1, server.requestsTo(userfieldPath, HttpMethod.Put).size)
        val stored = repository.locations.first().single { it.id == id }
        assertEquals(true, stored.isFreezer)
        assertEquals("3", stored.userfields["shelf"])
    }

    @Test
    fun savedChoreIsStoredAfterGrocyAssignedItsNextExecution() = runTest {
        repository.refresh()
        server.respond(
            "/api/objects/chores/1",
            body = """{"id":1,"name":"Vacuum the stairs","period_type":"daily"}""",
        )
        val chore = repository.chores.first().first { it.id == 1 }

        repository.saveChore(chore.copy(name = "Vacuum the stairs"), emptyMap())

        val calculate = "/api/chores/executions/calculate-next-assignments"
        assertEquals(1, server.requestsTo(calculate, HttpMethod.Post).size)
        assertEquals(1, server.requestsTo("/api/objects/chores/1", HttpMethod.Put).size)
        assertEquals("Vacuum the stairs", repository.chores.first().first { it.id == 1 }.name)
    }

    @Test
    fun rejectedSaveLeavesTheLocalCopy() = runTest {
        repository.refresh()
        server.respond(
            "/api/objects/batteries/1",
            HttpStatusCode.BadRequest,
            """{"error_message":"Name exists"}""",
            method = HttpMethod.Put,
        )
        val battery = repository.batteries.first().single()

        val error = assertFailsWith<GrocyApiException.HttpError> {
            repository.saveBattery(battery.copy(name = "Other"), emptyMap())
        }

        assertEquals("Name exists", error.serverMessage)
        assertEquals("Remote", repository.batteries.first().single().name)
    }

    @Test
    fun deletedObjectLeavesTheLocalCopy() = runTest {
        repository.refresh()

        repository.deleteObject(MasterDataRepository.LOCATIONS, 2)

        assertEquals(listOf("Fridge"), repository.locations.first().map { it.name })
        assertEquals(1, server.requestsTo("/api/objects/locations/2", HttpMethod.Delete).size)
    }

    @Test
    fun copiedProductIsStoredAndItsIdReturned() = runTest {
        repository.refresh()
        server.respond(
            "/api/stock/products/1/copy",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        server.respond(
            "/api/objects/products/${MasterDataJson.CREATED_ID}",
            body = MasterDataJson.CREATED_PRODUCT,
        )

        val id = repository.copyProduct(1)

        assertEquals(MasterDataJson.CREATED_ID, id)
        assertEquals("Created", repository.product(id).first()?.name)
    }

    @Test
    fun mergeDownloadsEverythingAgain() = runTest {
        repository.refresh()
        server.respond("/api/chores/1/merge/2", HttpStatusCode.NoContent, "", HttpMethod.Post)
        server.respond("/api/objects/chores", body = """[{"id":1,"name":"Vacuum","active":1}]""")

        repository.mergeChores(keepId = 1, removeId = 2)

        assertEquals(listOf("Vacuum"), repository.chores.first().map { it.name })
        assertEquals(1, server.requestsTo("/api/chores/1/merge/2", HttpMethod.Post).size)
    }
}
