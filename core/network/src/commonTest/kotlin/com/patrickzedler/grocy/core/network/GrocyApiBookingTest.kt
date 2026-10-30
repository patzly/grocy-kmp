package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.stock.BookingResult
import com.patrickzedler.grocy.core.model.stock.ConsumeBooking
import com.patrickzedler.grocy.core.model.stock.InventoryBooking
import com.patrickzedler.grocy.core.model.stock.OpenBooking
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.PurchaseBooking
import com.patrickzedler.grocy.core.model.stock.TransferBooking
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
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Grocy's stock pages: what they read of a product and what their bookings send. */
class GrocyApiBookingTest {

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

    private fun MockRequestHandleScope.json(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.json
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    private val booked =
        """[{"id":7,"product_id":1,"transaction_id":"tx1","stock_id":"s1","amount":"2"}]"""

    @Test
    fun detailsReadTheLastPurchaseAndTheNextDueDate() = runTest {
        val api = api {
            json(
                """{"stock_amount":"3","stock_amount_opened":"1","last_price":"0.5",""" +
                    """"last_shopping_location_id":"2","next_due_date":"2026-11-02 00:00:00",""" +
                    """"has_childs":true}""",
            )
        }

        val details = api.productStockDetails(CONNECTION, 1)

        assertEquals(3.0, details.stockAmount)
        assertEquals(1.0, details.stockAmountOpened)
        assertEquals(0.5, details.lastPrice)
        assertEquals(2, details.lastStoreId)
        assertEquals("2026-11-02", details.nextDueDate)
        assertTrue(details.hasSubProducts)
    }

    @Test
    fun locationsWithStockIncludeSubProductsOnRequest() = runTest {
        val api = api {
            json(
                """[{"product_id":1,"location_id":"3","location_name":"Fridge",""" +
                    """"amount":"2","location_is_freezer":"0"}]""",
            )
        }

        val locations = api.productLocationAmounts(CONNECTION, 1, includeSubProducts = true)

        assertEquals(listOf(ProductLocationAmount(3, "Fridge", 2.0, false)), locations)
        assertEquals("/api/stock/products/1/locations", requests.single().url.encodedPath)
        assertEquals("true", requests.single().url.parameters["include_sub_products"])
    }

    @Test
    fun purchaseSendsGrocysFieldsAndReturnsItsTransaction() = runTest {
        val api = api { json(booked) }

        val result = api.purchaseProduct(
            CONNECTION,
            1,
            PurchaseBooking(
                amount = 2.0,
                price = 0.75,
                bestBeforeDate = "2026-11-30",
                purchasedDate = null,
                storeId = 4,
                locationId = null,
                labelType = 1,
                note = "Sale",
            ),
        )

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/stock/products/1/add", request.url.encodedPath)
        assertEquals(2.0, request.json["amount"]?.jsonPrimitive?.double)
        assertEquals(0.75, request.json["price"]?.jsonPrimitive?.double)
        assertEquals("2026-11-30", request.json["best_before_date"]?.jsonPrimitive?.content)
        assertEquals(4, request.json["shopping_location_id"]?.jsonPrimitive?.int)
        assertEquals(1, request.json["stock_label_type"]?.jsonPrimitive?.int)
        assertEquals("Sale", request.json["note"]?.jsonPrimitive?.content)
        // Absent values let the server take the product's defaults
        assertFalse("location_id" in request.json)
        assertFalse("purchased_date" in request.json)
        assertEquals(BookingResult("tx1", "s1"), result)
    }

    @Test
    fun purchaseWithoutPriceSendsZeroLikeGrocysPage() = runTest {
        val api = api { json(booked) }

        api.purchaseProduct(
            CONNECTION,
            1,
            PurchaseBooking(1.0, null, null, null, null, null, null, ""),
        )

        assertEquals(0.0, requests.single().json["price"]?.jsonPrimitive?.double)
    }

    @Test
    fun consumeSendsTheChosenEntryAndLocation() = runTest {
        val api = api { json(booked) }

        api.consumeProduct(
            CONNECTION,
            1,
            ConsumeBooking(1.5, spoiled = true, stockEntryId = "s1", locationId = 3),
        )

        val body = requests.single().json
        assertEquals("/api/stock/products/1/consume", requests.single().url.encodedPath)
        assertTrue(body["spoiled"]!!.jsonPrimitive.boolean)
        assertTrue(body["allow_subproduct_substitution"]!!.jsonPrimitive.boolean)
        assertFalse(body["exact_amount"]!!.jsonPrimitive.boolean)
        assertEquals("s1", body["stock_entry_id"]?.jsonPrimitive?.content)
        assertEquals(3, body["location_id"]?.jsonPrimitive?.int)
        assertFalse("recipe_id" in body)
    }

    @Test
    fun openAndTransferPostToTheirEndpoints() = runTest {
        val api = api { json(booked) }

        api.openProduct(CONNECTION, 1, OpenBooking(1.0, locationId = 3))
        api.transferProduct(CONNECTION, 1, TransferBooking(2.0, 3, 5))

        assertEquals("/api/stock/products/1/open", requests[0].url.encodedPath)
        assertEquals("/api/stock/products/1/transfer", requests[1].url.encodedPath)
        assertEquals(3, requests[1].json["location_id_from"]?.jsonPrimitive?.int)
        assertEquals(5, requests[1].json["location_id_to"]?.jsonPrimitive?.int)
    }

    @Test
    fun inventoryWithoutPriceLetsTheServerTakeTheLastOne() = runTest {
        val api = api { json(booked) }

        api.inventoryProduct(
            CONNECTION,
            1,
            InventoryBooking(4.0, null, null, null, null, 2, null, ""),
        )

        val body = requests.single().json
        assertEquals("/api/stock/products/1/inventory", requests.single().url.encodedPath)
        assertEquals(4.0, body["new_amount"]?.jsonPrimitive?.double)
        assertFalse("price" in body)
        assertEquals(2, body["location_id"]?.jsonPrimitive?.int)
    }

    @Test
    fun recipesAreOnlyTheNormalOnes() = runTest {
        val api = api { json("""[{"id":3,"name":"Pancakes","type":"normal"}]""") }

        val recipes = api.recipes(CONNECTION)

        assertEquals(mapOf(3 to "Pancakes"), recipes)
        assertEquals("type=normal", requests.single().url.parameters["query[]"])
    }

    @Test
    fun bookingWithoutRowsHasNoTransaction() = runTest {
        val api = api { json("[]") }

        val result = api.transferProduct(CONNECTION, 1, TransferBooking(1.0, 1, 2))

        assertNull(result.transactionId)
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.test", "key")
    }
}
