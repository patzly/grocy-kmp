package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Grocy's shopping lists, their items and the actions of its shopping list page. */
class GrocyApiShoppingListTest {

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

    private fun MockRequestHandleScope.noContent() =
        respond("", HttpStatusCode.NoContent, headersOf())

    private val HttpRequestData.json
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun itemsAreReadWithDoneAndUnitWhichTheSpecLeavesOut() = runTest {
        val api = api {
            json(
                """[{"id":5,"shopping_list_id":"2","product_id":"1","note":"Low fat",""" +
                    """"amount":"6","qu_id":"2","done":"1","userfields":{"shop":"A"}},""" +
                    """{"id":6,"product_id":null,"note":"Batteries","amount":2,""" +
                    """"qu_id":null,"done":0,"userfields":null}]""",
            )
        }

        val items = api.shoppingListItems(CONNECTION)

        assertEquals("/api/objects/shopping_list", requests.single().url.encodedPath)
        assertEquals(
            listOf(
                ShoppingListItem(5, 2, 1, "Low fat", 6.0, 2, true, mapOf("shop" to "A")),
                // Grocy's first list is the column's default
                ShoppingListItem(6, 1, null, "Batteries", 2.0, null, false),
            ),
            items,
        )
    }

    @Test
    fun listsAreReadWithTheirNotes() = runTest {
        val api = api {
            json(
                """[{"id":1,"name":"Shopping list","description":"<p>Cash</p>"},""" +
                    """{"id":"2","name":"Hardware store","description":""}]""",
            )
        }

        val lists = api.shoppingLists(CONNECTION)

        assertEquals(
            listOf(
                ShoppingList(1, "Shopping list", "<p>Cash</p>"),
                ShoppingList(2, "Hardware store"),
            ),
            lists,
        )
    }

    @Test
    fun newItemIsCreatedWithTheFieldsOfGrocysForm() = runTest {
        val api = api { json("""{"created_object_id":12}""") }

        val id = api.saveShoppingListItem(
            CONNECTION,
            ShoppingListItem(0, 2, productId = null, note = "Batteries", 3.0, null, false),
        )

        assertEquals(12, id)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/objects/shopping_list", request.url.encodedPath)
        val body = request.json
        assertEquals(2, body.getValue("shopping_list_id").jsonPrimitive.int)
        assertEquals(JsonNull, body.getValue("product_id"))
        assertEquals(3.0, body.getValue("amount").jsonPrimitive.double)
        assertEquals(JsonNull, body.getValue("qu_id"))
        assertEquals("Batteries", body.getValue("note").jsonPrimitive.content)
        // Saving never touches whether the item is done
        assertFalse("done" in body)
    }

    @Test
    fun editedItemIsPutWithoutDone() = runTest {
        val api = api { noContent() }

        api.saveShoppingListItem(
            CONNECTION,
            ShoppingListItem(5, 1, productId = 1, note = " ", 6.0, 2, true),
        )

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/objects/shopping_list/5", request.url.encodedPath)
        assertEquals(JsonNull, request.json.getValue("note"))
        assertFalse("done" in request.json)
    }

    @Test
    fun checkingOffSendsOnlyDone() = runTest {
        val api = api { noContent() }

        api.setShoppingListItemDone(CONNECTION, id = 5, done = true)

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/objects/shopping_list/5", request.url.encodedPath)
        assertEquals("""{"done":1}""", (request.body as TextContent).text)
    }

    @Test
    fun checkingOffAGoneItemFails() = runTest {
        val api = api {
            json("""{"error_message":"Object not found"}""", HttpStatusCode.BadRequest)
        }

        val error = assertFailsWith<GrocyApiException.HttpError> {
            api.setShoppingListItemDone(CONNECTION, id = 5, done = false)
        }

        assertEquals("Object not found", error.serverMessage)
    }

    @Test
    fun listIsSavedByNameAndItsNotesSeparately() = runTest {
        val api = api { noContent() }

        api.saveShoppingList(CONNECTION, ShoppingList(2, "Hardware store", "<p>Old</p>"))
        api.saveShoppingListDescription(CONNECTION, listId = 2, description = "<p>New</p>")

        assertEquals("""{"name":"Hardware store"}""", (requests[0].body as TextContent).text)
        assertEquals("/api/objects/shopping_lists/2", requests[1].url.encodedPath)
        assertEquals("<p>New</p>", requests[1].json.getValue("description").jsonPrimitive.content)
    }

    @Test
    fun clearingSendsTheListAndWhetherOnlyDoneItems() = runTest {
        val api = api { noContent() }

        api.clearShoppingList(CONNECTION, listId = 2, doneOnly = true)

        val request = requests.single()
        assertEquals("/api/stock/shoppinglist/clear", request.url.encodedPath)
        assertEquals(2, request.json.getValue("list_id").jsonPrimitive.int)
        assertTrue(request.json.getValue("done_only").jsonPrimitive.boolean)
    }

    @Test
    fun stockActionsAddToTheChosenList() = runTest {
        val api = api { noContent() }

        api.addMissingProductsToShoppingList(CONNECTION, listId = 3)
        api.addOverdueProductsToShoppingList(CONNECTION, listId = 3)
        api.addExpiredProductsToShoppingList(CONNECTION, listId = 3)

        assertEquals(
            listOf(
                "/api/stock/shoppinglist/add-missing-products",
                "/api/stock/shoppinglist/add-overdue-products",
                "/api/stock/shoppinglist/add-expired-products",
            ),
            requests.map { it.url.encodedPath },
        )
        assertTrue(requests.all { it.json.getValue("list_id").jsonPrimitive.int == 3 })
    }

    @Test
    fun thermalPrintNamesListAndHeader() = runTest {
        val api = api { json("""{"result":"OK"}""") }

        api.printShoppingList(CONNECTION, listId = 2, printHeader = false)

        val url = requests.single().url
        assertEquals("/api/print/shoppinglist/thermal", url.encodedPath)
        assertEquals("2", url.parameters["list"])
        assertEquals("false", url.parameters["printHeader"])
    }

    @Test
    fun serverConfigAndSettingsReadThermalPrinterAndPrintHeader() = runTest {
        val api = api { request ->
            if (request.url.encodedPath.endsWith("config")) {
                json("""{"FEATURE_FLAG_THERMAL_PRINTER":"1"}""")
            } else {
                json("""{"shopping_list_print_show_header":"0"}""")
            }
        }

        assertTrue(api.serverConfig(CONNECTION).thermalPrinter)
        assertFalse(api.userSettings(CONNECTION).shoppingListPrintShowHeader)
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.test", "key")
    }
}
