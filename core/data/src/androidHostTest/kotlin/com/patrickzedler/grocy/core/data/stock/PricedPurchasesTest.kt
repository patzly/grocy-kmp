package com.patrickzedler.grocy.core.data.stock

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.stock.PricedPurchase
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/** The spendings' price history from the stock journal, read in the parts Grocy's view needs. */
class PricedPurchasesTest {

    private val queries = mutableListOf<List<String>>()
    private val engine = MockEngine { request ->
        val query = request.url.parameters.getAll("query[]").orEmpty()
        queries += query
        val body = when {
            "transaction_type=stock-edit-new" in query -> EDITS
            "stock_id=b" in query -> ENTRY_B
            query.any { it.startsWith("purchased_date>=") } -> DATED
            else -> "[]"
        }
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    private val database = createInMemoryGrocyDatabase()
    private val repository = StockRepository(
        GrocyApi(createGrocyHttpClient(engine), apiKeyAsQueryParameter = false),
        database,
    ) { ServerConnection.ApiKey("https://grocy.test", "k") }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun editedEntriesInTheRangeAreReadCompletely() = runTest {
        val purchases = repository.pricedPurchases("2026-11-01", "2026-11-30")

        assertEquals(
            listOf(
                PricedPurchase(1, 2.0, 3.0, "2026-11-02", 5, "purchase"),
                // Edited to 1 at 4.0 after 2 were consumed
                PricedPurchase(2, 4.0, 3.0, "2026-11-10", null, "stock-edit-new"),
            ),
            purchases.sortedBy { it.productId },
        )
        assertEquals(
            listOf("purchased_date>=2026-11-01", "purchased_date<=2026-11-30"),
            queries.first { query -> query.any { it.startsWith("purchased_date") } },
        )
        // The entry edited outside the range is not read
        assertEquals(1, queries.count { query -> query.any { it.startsWith("stock_id=") } })
    }

    private companion object {
        const val DATED = """[
            {"id":1,"product_id":1,"stock_id":"a","transaction_type":"purchase","amount":"3",
             "price":"2","purchased_date":"2026-11-02","shopping_location_id":"5","undone":"0"},
            {"id":2,"product_id":2,"stock_id":"b","transaction_type":"purchase","amount":"3",
             "price":"1","purchased_date":"2026-11-01","undone":"0"}
        ]"""
        const val EDITS = """[
            {"id":4,"product_id":2,"stock_id":"b","transaction_type":"stock-edit-new","amount":"1",
             "price":"4","purchased_date":"2026-11-10","undone":"0"},
            {"id":7,"product_id":3,"stock_id":"c","transaction_type":"stock-edit-new","amount":"1",
             "price":"4","purchased_date":"2026-10-10","undone":"0"}
        ]"""
        const val ENTRY_B = """[
            {"id":2,"product_id":2,"stock_id":"b","transaction_type":"purchase","amount":"3",
             "price":"1","purchased_date":"2026-11-01","undone":"0"},
            {"id":3,"product_id":2,"stock_id":"b","transaction_type":"consume","amount":"-2",
             "price":"1","purchased_date":"2026-11-01","undone":"0"},
            {"id":4,"product_id":2,"stock_id":"b","transaction_type":"stock-edit-new","amount":"1",
             "price":"4","purchased_date":"2026-11-10","undone":"0"}
        ]"""
    }
}
