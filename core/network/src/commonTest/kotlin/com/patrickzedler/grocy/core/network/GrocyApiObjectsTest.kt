package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GrocyApiObjectsTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        GrocyApi(
            httpClient = createGrocyHttpClient(
                MockEngine { request ->
                    requests += request
                    handler(request)
                },
            ),
            ingressSessionSource = { error("No ingress in these tests") },
            apiKeyAsQueryParameter = false,
        )

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.json
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun choresAreReadWithWeekdaysAssignedUsersAndUserfields() = runTest {
        val api = api {
            json(
                """[{"id":"4","name":"Vacuum","period_type":"weekly",""" +
                    """"period_config":"monday,friday","period_interval":"2",""" +
                    """"assignment_type":"random","assignment_config":"1,3",""" +
                    """"start_date":"2026-01-01","rollover":"1","product_id":"",""" +
                    """"userfields":{"room":"Kitchen"}}]""",
            )
        }

        val chore = api.chores(CONNECTION).single()

        assertEquals(4, chore.id)
        assertEquals(ChorePeriodType.WEEKLY, chore.periodType)
        assertEquals(setOf(Weekday.MONDAY, Weekday.FRIDAY), chore.weekdays)
        assertEquals(2, chore.periodInterval)
        assertEquals(ChoreAssignmentType.RANDOM, chore.assignmentType)
        assertEquals(listOf(1, 3), chore.assignedUserIds)
        assertTrue(chore.rollover)
        assertEquals(null, chore.productId)
        assertEquals(mapOf("room" to "Kitchen"), chore.userfields)
    }

    @Test
    fun newLocationIsPostedAndGetsGrocysId() = runTest {
        val api = api { json("""{"created_object_id":"8"}""") }

        val id = api.saveLocation(CONNECTION, Location(id = 0, name = "Cellar", isFreezer = true))

        val request = requests.single()
        assertEquals(8, id)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/objects/locations", request.url.encodedPath)
        assertEquals("application/json", request.body.contentType.toString())
        assertEquals("Cellar", request.json["name"]?.jsonPrimitive?.content)
        assertEquals(1, request.json["is_freezer"]?.jsonPrimitive?.int)
        assertEquals(1, request.json["active"]?.jsonPrimitive?.int)
        assertFalse("id" in request.json)
        assertFalse("userfields" in request.json)
    }

    @Test
    fun editedStoreIsPutToGrocysShoppingLocations() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        val id = api.saveStore(CONNECTION, Store(id = 5, name = "Market", active = false))

        assertEquals(5, id)
        assertEquals(HttpMethod.Put, requests.single().method)
        assertEquals("/api/objects/shopping_locations/5", requests.single().url.encodedPath)
        assertEquals(0, requests.single().json["active"]?.jsonPrimitive?.int)
    }

    @Test
    fun savedChoreLetsGrocyAssignItsNextExecution() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.saveChore(CONNECTION, Chore(id = 3, name = "Vacuum"))

        assertEquals(listOf(HttpMethod.Put, HttpMethod.Post), requests.map { it.method })
        assertEquals("/api/objects/chores/3", requests[0].url.encodedPath)
        assertEquals(
            "/api/chores/executions/calculate-next-assignments",
            requests[1].url.encodedPath,
        )
        assertEquals(3, requests[1].json["chore_id"]?.jsonPrimitive?.int)
    }

    @Test
    fun onlyWeeklyChoresSendTheirWeekdays() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }
        val weekdays = setOf(Weekday.SUNDAY, Weekday.MONDAY)
        val weekly = Chore(0, "Clean", periodType = ChorePeriodType.WEEKLY, weekdays = weekdays)

        api.saveChore(CONNECTION, weekly.copy(id = 1))
        api.saveChore(CONNECTION, weekly.copy(id = 2, periodType = ChorePeriodType.DAILY))

        val puts = requests.filter { it.method == HttpMethod.Put }
        assertEquals("monday,sunday", puts[0].json["period_config"]?.jsonPrimitive?.content)
        assertEquals("", puts[1].json["period_config"]?.jsonPrimitive?.content)
    }

    @Test
    fun choresSendTheirAssignmentAndProductLikeGrocysForm() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.saveChore(
            CONNECTION,
            Chore(
                id = 1,
                name = "Laundry",
                assignmentType = ChoreAssignmentType.IN_ALPHABETICAL_ORDER,
                assignedUserIds = listOf(2, 5),
                startDate = "2026-10-24 08:00:00",
            ),
        )

        val body = requests.first().json
        assertEquals("in-alphabetical-order", body["assignment_type"]?.jsonPrimitive?.content)
        assertEquals("2,5", body["assignment_config"]?.jsonPrimitive?.content)
        assertEquals("2026-10-24 08:00:00", body["start_date"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, body["product_id"])
    }

    @Test
    fun userfieldIsSentWithItsOptionsAsConfiguration() = runTest {
        val api = api { json("""{"created_object_id":3}""") }
        val userfield = Userfield(
            id = 0,
            entity = "chores",
            name = "room",
            caption = "Room",
            type = UserfieldType.PRESET_LIST,
            options = listOf("Kitchen", "Bath"),
            sortNumber = 2,
        )

        api.saveUserfield(CONNECTION, userfield)

        val body = requests.single().json
        assertEquals("/api/objects/userfields", requests.single().url.encodedPath)
        assertEquals("preset-list", body["type"]?.jsonPrimitive?.content)
        assertEquals("Kitchen\nBath", body["config"]?.jsonPrimitive?.content)
        assertEquals(2, body["sort_number"]?.jsonPrimitive?.int)
        assertEquals(0, body["input_required"]?.jsonPrimitive?.int)
    }

    @Test
    fun userentitiesWithoutCaptionAreCalledByTheirName() = runTest {
        val api = api {
            json("""[{"id":1,"name":"books","caption":"","show_in_sidebar_menu":"1"}]""")
        }

        val userentity = api.userentities(CONNECTION).single()

        assertEquals("books", userentity.caption)
        assertTrue(userentity.showInSidebarMenu)
    }

    @Test
    fun quantityUnitsKeepTheirPluralForms() = runTest {
        val api = api {
            json("""[{"id":1,"name":"Kus","plural_forms":"Kus\nKusy\nKusu","userfields":null}]""")
        }

        val unit = api.quantityUnits(CONNECTION).single()

        assertEquals("Kus\nKusy\nKusu", unit.pluralForms)
        assertEquals(emptyMap(), unit.userfields)
    }

    @Test
    fun choreAssignmentsFeatureIsRead() = runTest {
        val api = api { json("""{"FEATURE_FLAG_CHORES_ASSIGNMENTS":"0"}""") }

        assertFalse(api.serverConfig(CONNECTION).choresAssignments)
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.test", "key")
    }

    @Test
    fun objectsAreDeletedThroughTheirObjectsPath() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.deleteObject(CONNECTION, "locations", 3)

        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("/api/objects/locations/3", requests.single().url.encodedPath)
    }

    @Test
    fun copiedProductReturnsTheIdOfTheCopy() = runTest {
        val api = api { json("""{"created_object_id":"12"}""") }

        val id = api.copyProduct(CONNECTION, 4)

        assertEquals(12, id)
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("/api/stock/products/4/copy", requests.single().url.encodedPath)
    }

    @Test
    fun mergesPostTheKeptAndTheRemovedId() = runTest {
        val api = api { json("", HttpStatusCode.NoContent) }

        api.mergeProducts(CONNECTION, keepId = 1, removeId = 2)
        api.mergeChores(CONNECTION, keepId = 3, removeId = 4)

        assertEquals(
            listOf("/api/stock/products/1/merge/2", "/api/chores/3/merge/4"),
            requests.map { it.url.encodedPath },
        )
        assertTrue(requests.all { it.method == HttpMethod.Post })
    }
}
