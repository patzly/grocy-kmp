package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.stock.StockEntry
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Barcodes, conversions, stock entries, the journal, shopping lists and user settings. */
class GrocyApiStockTest {

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
    fun barcodeIsSavedLikeGrocysFormWithoutTheLastPrice() = runTest {
        val api = api { json("""{"created_object_id":7}""") }
        val barcode = ProductBarcode(
            id = 0,
            productId = 1,
            barcode = "4006381333931",
            quId = 2,
            amount = 6.0,
            storeId = null,
            lastPrice = 1.5,
            note = "Six pack",
        )

        val id = api.saveProductBarcode(CONNECTION, barcode)

        assertEquals(7, id)
        val body = requests.single().json
        assertEquals("/api/objects/product_barcodes", requests.single().url.encodedPath)
        assertEquals(1, body.getValue("product_id").jsonPrimitive.int)
        assertEquals("4006381333931", body.getValue("barcode").jsonPrimitive.content)
        assertEquals(2, body.getValue("qu_id").jsonPrimitive.int)
        assertEquals(6.0, body.getValue("amount").jsonPrimitive.double)
        assertEquals(JsonNull, body.getValue("shopping_location_id"))
        assertEquals("Six pack", body.getValue("note").jsonPrimitive.content)
        assertFalse("last_price" in body)
    }

    @Test
    fun barcodesAreReadWithTheirUserfields() = runTest {
        val api = api {
            json(
                """[{"id":"3","product_id":"1","barcode":"123","qu_id":"","amount":null,""" +
                    """"userfields":{"size":"XL"}}]""",
            )
        }

        val barcode = api.productBarcodes(CONNECTION).single()

        assertEquals(3, barcode.id)
        assertNull(barcode.quId)
        assertEquals(mapOf("size" to "XL"), barcode.userfields)
    }

    @Test
    fun defaultConversionIsSentWithoutProduct() = runTest {
        val api = api { json("""{"created_object_id":4}""") }

        api.saveQuantityUnitConversion(CONNECTION, QuantityUnitConversion(0, 2, 1, 6.0))

        val body = requests.single().json
        assertEquals(2, body.getValue("from_qu_id").jsonPrimitive.int)
        assertEquals(1, body.getValue("to_qu_id").jsonPrimitive.int)
        assertEquals(6.0, body.getValue("factor").jsonPrimitive.double)
        assertFalse("product_id" in body)
    }

    @Test
    fun overrideOfAProductIsUpdatedWithItsProduct() = runTest {
        val api = api { respond("", HttpStatusCode.NoContent) }

        api.saveQuantityUnitConversion(CONNECTION, QuantityUnitConversion(5, 2, 1, 10.0, 3))

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/objects/quantity_unit_conversions/5", request.url.encodedPath)
        assertEquals(3, request.json.getValue("product_id").jsonPrimitive.int)
    }

    @Test
    fun resolvedConversionsOfAllProductsHaveNoProduct() = runTest {
        val api = api {
            json(
                """[{"id":1,"product_id":"","from_qu_id":"2","to_qu_id":"1","factor":"6"},""" +
                    """{"id":2,"product_id":4,"from_qu_id":1,"to_qu_id":2,"factor":0.5}]""",
            )
        }

        val resolved = api.resolvedQuantityUnitConversions(CONNECTION)

        assertNull(resolved[0].productId)
        assertEquals(6.0, resolved[0].factor)
        assertEquals(4, resolved[1].productId)
    }

    @Test
    fun stockEntriesAreReadWithTheirStockIdAndUserfields() = runTest {
        val api = api {
            json(
                """[{"id":"1","product_id":"2","amount":"1.5","best_before_date":"2999-12-31",""" +
                    """"purchased_date":"","stock_id":"abc","price":"","open":"1",""" +
                    """"location_id":"3","shopping_location_id":null,"note":"",""" +
                    """"userfields":{"batch":"7"}}]""",
            )
        }

        val entry = api.stockEntries(CONNECTION).single()

        assertEquals(1.5, entry.amount)
        assertEquals(StockEntry.NEVER_OVERDUE, entry.bestBeforeDate)
        assertNull(entry.purchasedDate)
        assertEquals("abc", entry.stockId)
        assertNull(entry.price)
        assertTrue(entry.open)
        assertNull(entry.note)
        assertEquals(mapOf("batch" to "7"), entry.userfields)
    }

    @Test
    fun wholeStockEntryIsConsumedExactlyAndItsBookingReturned() = runTest {
        val api = api { json("""[{"id":12,"product_id":2,"amount":"-2"}]""") }

        val booking = api.consumeStockEntry(CONNECTION, ENTRY, spoiled = true)

        assertEquals(12, booking)
        val request = requests.single()
        assertEquals("/api/stock/products/2/consume", request.url.encodedPath)
        val body = request.json
        assertEquals(2.0, body.getValue("amount").jsonPrimitive.double)
        assertTrue(body.getValue("spoiled").jsonPrimitive.boolean)
        assertEquals(3, body.getValue("location_id").jsonPrimitive.int)
        assertEquals("abc", body.getValue("stock_entry_id").jsonPrimitive.content)
        assertTrue(body.getValue("exact_amount").jsonPrimitive.boolean)
    }

    @Test
    fun wholeStockEntryIsOpened() = runTest {
        val api = api { json("""[{"id":13,"product_id":2}]""") }

        val booking = api.openStockEntry(CONNECTION, ENTRY)

        assertEquals(13, booking)
        val body = requests.single().json
        assertEquals("/api/stock/products/2/open", requests.single().url.encodedPath)
        assertEquals(2.0, body.getValue("amount").jsonPrimitive.double)
        assertEquals("abc", body.getValue("stock_entry_id").jsonPrimitive.content)
    }

    @Test
    fun editedStockEntrySendsEveryFieldOfGrocysForm() = runTest {
        val api = api { json("""[{"id":14,"product_id":2}]""") }

        api.editStockEntry(CONNECTION, ENTRY.copy(price = 0.5, storeId = 4, note = null))

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/stock/entry/1", request.url.encodedPath)
        val body = request.json
        assertEquals("2026-11-01", body.getValue("best_before_date").jsonPrimitive.content)
        assertEquals("2026-10-01", body.getValue("purchased_date").jsonPrimitive.content)
        assertEquals(0.5, body.getValue("price").jsonPrimitive.double)
        assertFalse(body.getValue("open").jsonPrimitive.boolean)
        assertEquals(3, body.getValue("location_id").jsonPrimitive.int)
        assertEquals(4, body.getValue("shopping_location_id").jsonPrimitive.int)
        assertEquals("", body.getValue("note").jsonPrimitive.content)
    }

    @Test
    fun stockEntryUserfieldsBelongToItsStockId() = runTest {
        val api = api { respond("", HttpStatusCode.NoContent) }

        api.setUserfields(CONNECTION, "stock", "abc", mapOf("batch" to "8"))

        assertEquals("/api/userfields/stock/abc", requests.single().url.encodedPath)
    }

    @Test
    fun journalIsAskedForItsTimeNewestFirst() = runTest {
        val api = api {
            json(
                """[{"id":"5","product_id":"2","amount":"-1","transaction_type":"consume",""" +
                    """"row_created_timestamp":"2026-10-02 10:00:00","spoiled":"1",""" +
                    """"undone":"1","undone_timestamp":"2026-10-02 10:01:00","user_id":"1",""" +
                    """"correlation_id":"","note":null}]""",
            )
        }

        val booking = api.stockLog(CONNECTION, since = "2026-04-25", productId = 2).single()

        val parameters = requests.single().url.parameters
        assertEquals(
            listOf("row_created_timestamp>=2026-04-25", "product_id=2"),
            parameters.getAll("query[]"),
        )
        assertEquals("row_created_timestamp:desc", parameters["order"])
        assertEquals(-1.0, booking.amount)
        assertTrue(booking.spoiled)
        assertTrue(booking.undone)
        assertEquals("2026-10-02 10:01:00", booking.undoneTimestamp)
        assertNull(booking.correlationId)
    }

    @Test
    fun wholeJournalIsAskedWithoutFilter() = runTest {
        val api = api { json("[]") }

        api.stockLog(CONNECTION, since = null)

        assertNull(requests.single().url.parameters.getAll("query[]"))
    }

    @Test
    fun undoingABookingPostsToIt() = runTest {
        val api = api { respond("", HttpStatusCode.NoContent) }

        api.undoBooking(CONNECTION, 12)

        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("/api/stock/bookings/12/undo", requests.single().url.encodedPath)
    }

    @Test
    fun productIsAddedToTheShoppingListAndItsItemFound() = runTest {
        val api = api { request ->
            if (request.method == HttpMethod.Post) {
                respond("", HttpStatusCode.NoContent)
            } else {
                json("""[{"id":"9","product_id":"2","shopping_list_id":"3"}]""")
            }
        }

        val itemId = api.addProductToShoppingList(CONNECTION, 2, 3, 1.5, 4, note = " ")

        assertEquals(9, itemId)
        val body = requests.first().json
        assertEquals("/api/stock/shoppinglist/add-product", requests.first().url.encodedPath)
        assertEquals(2, body.getValue("product_id").jsonPrimitive.int)
        assertEquals(3, body.getValue("list_id").jsonPrimitive.int)
        assertEquals(1.5, body.getValue("product_amount").jsonPrimitive.double)
        assertEquals(4, body.getValue("qu_id").jsonPrimitive.int)
        assertFalse("note" in body)
        assertEquals(
            listOf("product_id=2", "shopping_list_id=3"),
            requests.last().url.parameters.getAll("query[]"),
        )
    }

    @Test
    fun userSettingIsSavedAsValue() = runTest {
        val api = api { respond("", HttpStatusCode.NoContent) }

        api.saveUserSetting(CONNECTION, "stock_due_soon_days", UserSettingValue.Whole(7))

        val request = requests.single()
        assertEquals("/api/user/settings/stock_due_soon_days", request.url.encodedPath)
        assertEquals(7, request.json.getValue("value").jsonPrimitive.int)
    }

    @Test
    fun labelsArePrintedByTheServer() = runTest {
        val api = api { json("{}") }

        api.printProductLabel(CONNECTION, 2)
        api.printStockEntryLabel(CONNECTION, 1)

        assertEquals(
            listOf("/api/stock/products/2/printlabel", "/api/stock/entry/1/printlabel"),
            requests.map { it.url.encodedPath },
        )
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.test", "key")

        val ENTRY = StockEntry(
            id = 1,
            productId = 2,
            amount = 2.0,
            bestBeforeDate = "2026-11-01",
            purchasedDate = "2026-10-01",
            stockId = "abc",
            locationId = 3,
        )
    }
}
