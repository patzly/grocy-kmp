package com.patrickzedler.grocy.feature.stock.impl.scan

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import com.patrickzedler.grocy.feature.stock.impl.booking.BookingPreset
import com.patrickzedler.grocy.feature.stock.impl.booking.BookingViewModel
import io.ktor.http.HttpMethod
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class StockScanViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server.respond(
            "/api/stock/products/1/locations",
            body = """[{"location_id":1,"location_name":"Fridge","amount":"3"}]""",
        )
        server.respond(
            "/api/stock/products/1/entries",
            body = """[{"id":2,"product_id":1,"amount":"3","stock_id":"b","location_id":1,""" +
                """"open":0}]""",
        )
        server.respond(
            "/api/stock/products/1/consume",
            body = """[{"id":9,"product_id":1,"transaction_id":"tx1","stock_id":"s1"}]""",
            method = HttpMethod.Post,
        )
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(): StockScanViewModel {
        environment.repository.refresh()
        return StockScanViewModel(environment.repository) { kind, code ->
            BookingViewModel(
                kind = kind,
                masterData = environment.repository,
                stock = environment.stockRepository,
                preset = BookingPreset(code = code),
                decimalSeparator = '.',
                scanner = ScannerFieldState(hasValue = true),
            )
        }.also { advanceUntilIdle() }
    }

    @Test
    fun scannedBarcodeShowsItsProductAndTheBookingsItAllows() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onScanned(" $MILK_BARCODE ")

        assertEquals(MILK_BARCODE, viewModel.code)
        val options = viewModel.options.value!!
        val scanned = assertIs<ScannedCode.Found>(scannedCodeOf(MILK_BARCODE, options))
        assertEquals("Milk", scanned.product.name)
        assertEquals("3 Pieces", stockAmountText(scanned, options, "en", '.'))
        assertEquals(BookingKind.entries, offeredKinds(scanned, options))
        assertNull(viewModel.booking)
    }

    @Test
    fun productWithoutStockOffersOnlyToAddIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        val options = viewModel.options.value!!

        val bread = scannedCodeOf("Bread", options)

        assertEquals(
            listOf(BookingKind.PURCHASE, BookingKind.INVENTORY),
            offeredKinds(bread, options),
        )
        assertIs<ScannedCode.Unknown>(scannedCodeOf("0000", options))
    }

    @Test
    fun chosenBookingOpensWithTheScannedProduct() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onScanned(MILK_BARCODE)

        viewModel.choose(BookingKind.CONSUME)
        advanceUntilIdle()

        val booking = viewModel.booking!!
        assertEquals(BookingKind.CONSUME, booking.kind)
        assertEquals("Milk", booking.form.product?.name)
    }

    @Test
    fun anotherChoiceReplacesTheUnbookedPage() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onScanned(MILK_BARCODE)
        viewModel.choose(BookingKind.CONSUME)
        val consume = viewModel.booking

        viewModel.choose(BookingKind.PURCHASE)
        advanceUntilIdle()

        assertNotSame(consume, viewModel.booking)
        assertEquals(BookingKind.PURCHASE, viewModel.booking?.kind)
    }

    @Test
    fun bookingScansAgainAndKeepsThePageForItsUndo() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onScanned(MILK_BARCODE)
        viewModel.choose(BookingKind.CONSUME)
        advanceUntilIdle()
        val consume = viewModel.booking!!
        // The barcode is of a pack of 6, one piece is what the stock has room for
        consume.setUnit(1)
        consume.form.amount.setTextAndPlaceCursorAtEnd("1")

        assertTrue(consume.book())
        advanceUntilIdle()
        viewModel.onBooked()

        assertNull(viewModel.code)
        assertNull(viewModel.booking)
        assertSame(consume, viewModel.previousBooking)
    }

    @Test
    fun typedCodeWorksLikeAScannedOne() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.codeField.setTextAndPlaceCursorAtEnd(MILK_BARCODE)

        viewModel.onCodeSubmitted()

        assertEquals(MILK_BARCODE, viewModel.code)
        assertEquals("", viewModel.codeField.text.toString())
    }

    private companion object {
        const val MILK_BARCODE = "4006381333931"
    }
}
