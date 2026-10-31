package com.patrickzedler.grocy.feature.stock.impl.booking

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BookingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server
    private val removedItems = mutableListOf<Int>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val booked = """[{"id":9,"product_id":1,"transaction_id":"tx1","stock_id":"s1"}]"""
        for (action in listOf("add", "consume", "open", "transfer", "inventory")) {
            server.respond("/api/stock/products/1/$action", body = booked, method = HttpMethod.Post)
        }
        server.respond(
            "/api/stock/products/1/locations",
            body = """[{"location_id":1,"location_name":"Fridge","amount":"3"},""" +
                """{"location_id":2,"location_name":"Pantry","amount":"0"}]""",
        )
        server.respond(
            "/api/stock/products/1/entries",
            body = """[{"id":1,"product_id":1,"amount":"1","stock_id":"a","location_id":1,""" +
                """"open":1},{"id":2,"product_id":1,"amount":"2","stock_id":"b",""" +
                """"location_id":1,"open":0}]""",
        )
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(
        kind: BookingKind,
        preset: BookingPreset = BookingPreset(),
    ): BookingViewModel {
        environment.repository.refresh()
        return BookingViewModel(
            kind = kind,
            masterData = environment.repository,
            stock = environment.stockRepository,
            preset = preset,
            removeShoppingListItem = { removedItems += it },
            today = { epochDayOf("2026-10-30")!! },
            decimalSeparator = '.',
        ).also { advanceUntilIdle() }
    }

    private fun body(action: String) = server
        .requestsTo("/api/stock/products/1/$action", HttpMethod.Post)
        .map { Json.parseToJsonElement((it.body as TextContent).text).jsonObject }

    @Test
    fun purchasePrefillsThePurchaseUnitAndItsLastPrice() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.PURCHASE)

        viewModel.selectProduct(1)
        advanceUntilIdle()

        val form = viewModel.form
        // Milk is purchased in packs of 6, its last price was 1 per piece
        assertEquals(2, form.quId)
        assertEquals("6", form.price.text.toString())
        assertIs<ProductContext.Ready>(form.context)
    }

    @Test
    fun purchaseBooksTheAmountAndPriceInTheStockUnit() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.PURCHASE)
        viewModel.selectProduct(1)
        advanceUntilIdle()
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("2")
        viewModel.form.dueDate.setTextAndPlaceCursorAtEnd("2026-11-30")

        viewModel.events.test {
            assertTrue(viewModel.book())
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<BookingEvent.Booked>(event)
            assertEquals("tx1", event.transactionId)
            assertEquals(12.0, event.amount, 1e-6)
            cancelAndIgnoreRemainingEvents()
        }
        val body = body("add").single()
        assertEquals(12.0, body["amount"]!!.jsonPrimitive.double, 1e-6)
        assertEquals(1.0, body["price"]!!.jsonPrimitive.double, 1e-6)
        assertEquals("2026-11-30", body["best_before_date"]!!.jsonPrimitive.content)
        // The form is ready for the next product
        assertNull(viewModel.form.product)
    }

    @Test
    fun purchaseWithoutAmountIsNotSent() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.PURCHASE)
        viewModel.selectProduct(1)
        advanceUntilIdle()

        assertFalse(viewModel.book())

        assertIs<BookingError.InvalidAmount>(viewModel.form.errors[BookingField.AMOUNT])
        assertTrue(body("add").isEmpty())
    }

    @Test
    fun consumePrefillsTheLocationWithStock() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.CONSUME)

        viewModel.selectProduct(1)
        advanceUntilIdle()

        assertEquals(1, viewModel.form.locationId)
        assertEquals("1", viewModel.form.amount.text.toString())
    }

    @Test
    fun consumeBooksAtTheLocationAndRefusesMoreThanItHas() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.CONSUME)
        viewModel.selectProduct(1)
        advanceUntilIdle()
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("4")

        assertFalse(viewModel.book())
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("2")
        assertTrue(viewModel.book())
        advanceUntilIdle()

        val body = body("consume").single()
        assertEquals(2.0, body["amount"]!!.jsonPrimitive.double)
        assertEquals(1, body["location_id"]!!.jsonPrimitive.int)
    }

    @Test
    fun aStockEntrysGrocycodeChoosesTheWholeEntry() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.CONSUME)

        viewModel.onCode("grcy:p:1:b", scanMode = false)
        advanceUntilIdle()

        assertTrue(viewModel.form.useSpecificEntry)
        assertEquals("b", viewModel.form.stockId)
        assertEquals("2", viewModel.form.amount.text.toString())
    }

    @Test
    fun onlyUnopenedStockCanBeOpened() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.CONSUME)
        viewModel.selectProduct(1)
        advanceUntilIdle()
        val options = viewModel.options.value!!

        viewModel.form.amount.setTextAndPlaceCursorAtEnd("2")
        assertTrue(viewModel.canOpen(options))
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("3")
        assertFalse(viewModel.canOpen(options))
    }

    @Test
    fun transferNeedsBothLocations() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.TRANSFER)
        viewModel.selectProduct(1)
        advanceUntilIdle()
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("1")

        assertFalse(viewModel.book())
        assertEquals(BookingError.Required, viewModel.form.errors[BookingField.TO_LOCATION])
        viewModel.form.toLocationId = 2
        assertTrue(viewModel.book())
        advanceUntilIdle()

        val body = body("transfer").single()
        assertEquals(1, body["location_id_from"]!!.jsonPrimitive.int)
        assertEquals(2, body["location_id_to"]!!.jsonPrimitive.int)
    }

    @Test
    fun inventoryStartsWithTheStockAndNeedsAChange() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.INVENTORY)
        viewModel.selectProduct(1)
        advanceUntilIdle()

        assertEquals("3", viewModel.form.amount.text.toString())
        assertFalse(viewModel.book())
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("1")
        assertEquals(InventoryChange.Removed(2.0), viewModel.inventoryChange())
        assertTrue(viewModel.book())
        advanceUntilIdle()

        assertEquals(1.0, body("inventory").single()["new_amount"]!!.jsonPrimitive.double)
    }

    @Test
    fun aPurchaseFromTheShoppingListKnowsItsPlaceInTheWorkflow() = runTest(dispatcher) {
        val viewModel = viewModel(
            BookingKind.PURCHASE,
            BookingPreset(
                productId = 1,
                shoppingListItemId = 5,
                shoppingListPosition = 2,
                shoppingListCount = 4,
            ),
        )

        assertEquals(ShoppingListItemPurchase(position = 2, count = 4), viewModel.shoppingListItem)
    }

    @Test
    fun otherBookingsHaveNoShoppingListHint() = runTest(dispatcher) {
        assertNull(viewModel(BookingKind.PURCHASE, BookingPreset(productId = 1)).shoppingListItem)
        assertNull(viewModel(BookingKind.CONSUME).shoppingListItem)
    }

    @Test
    fun aShoppingListItemIsRemovedOnceItIsInStock() = runTest(dispatcher) {
        val viewModel = viewModel(
            BookingKind.PURCHASE,
            BookingPreset(productId = 1, amount = 12.0, quId = 2, shoppingListItemId = 5),
        )
        // The item's 12 pieces as its 2 packs
        assertEquals("2", viewModel.form.amount.text.toString())
        viewModel.form.dueDate.setTextAndPlaceCursorAtEnd("2026-11-30")

        viewModel.events.test {
            viewModel.book()
            advanceUntilIdle()

            assertIs<BookingEvent.Booked>(awaitItem())
            assertEquals(BookingEvent.Done, awaitItem())
        }
        assertEquals(listOf(5), removedItems)
    }

    @Test
    fun unknownBarcodesAreAddedToTheChosenProductWithTheBooking() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.PURCHASE)

        viewModel.onCode("123456", scanMode = false)
        assertEquals("123456", viewModel.unknownCode)
        viewModel.assignUnknownCode()
        viewModel.selectProduct(1)
        advanceUntilIdle()
        viewModel.form.amount.setTextAndPlaceCursorAtEnd("1")
        viewModel.form.dueDate.setTextAndPlaceCursorAtEnd("2026-11-30")
        viewModel.book()
        advanceUntilIdle()

        val barcode = server.requestsTo("/api/objects/product_barcodes", HttpMethod.Post)
            .single()
            .let { Json.parseToJsonElement((it.body as TextContent).text).jsonObject }
        assertEquals("123456", barcode["barcode"]!!.jsonPrimitive.content)
        assertEquals(1, barcode["product_id"]!!.jsonPrimitive.int)
        assertNull(viewModel.form.pendingBarcode)
    }

    @Test
    fun offlineProductsCannotBeBooked() = runTest(dispatcher) {
        val viewModel = viewModel(BookingKind.CONSUME)
        server.unreachable = true

        viewModel.selectProduct(1)
        advanceUntilIdle()

        assertIs<ProductContext.Failed>(viewModel.form.context)
        assertFalse(viewModel.book())
    }
}
