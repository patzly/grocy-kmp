package com.patrickzedler.grocy.feature.masterdata.impl.barcode

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ProductBarcodesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = MasterDataTestEnvironment(dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(): ProductBarcodesViewModel {
        environment.repository.refresh()
        return ProductBarcodesViewModel(environment.repository, decimalSeparator = '.').also {
            backgroundScope.launch { it.items.collect {} }
            it.showProduct(1)
            advanceUntilIdle()
        }
    }

    @Test
    fun productShowsItsBarcodes() = runTest(dispatcher) {
        val viewModel = viewModel()

        val item = viewModel.items.value.single()
        assertEquals("4006381333931", item.barcode)
        assertEquals("1 Pack", item.amount)
    }

    @Test
    fun newBarcodeOffersTheUnitsOfThePurchaseUnit() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.open()
        advanceUntilIdle()

        val dialog = assertNotNull(viewModel.dialog)
        assertTrue(dialog.isNew)
        assertEquals("Milk", dialog.productName)
        assertEquals(listOf("Pack", "Piece"), dialog.units.map { it.unit.name })
        assertEquals("Pack", dialog.purchaseUnit?.name)
    }

    @Test
    fun missingBarcodeIsNotSent() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.open()
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.dialog?.errors?.get(BarcodeField.BARCODE))
        assertTrue(
            environment.server.requestsTo("/api/objects/product_barcodes", HttpMethod.Post)
                .isEmpty(),
        )
    }

    @Test
    fun editedBarcodeIsSavedAndTheDialogCloses() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/product_barcodes/1",
            body = """{"id":1,"product_id":1,"barcode":"4006381333931","note":"Six pack"}""",
        )
        val viewModel = viewModel()
        viewModel.open(1)
        advanceUntilIdle()

        viewModel.dialog!!.note.setTextAndPlaceCursorAtEnd("Six pack")
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        val put = environment.server.requestsTo("/api/objects/product_barcodes/1", HttpMethod.Put)
        assertTrue("\"note\":\"Six pack\"" in (put.single().body as TextContent).text)
        assertEquals("Six pack", viewModel.items.value.single().note)
    }

    @Test
    fun rejectedBarcodeKeepsTheDialogWithGrocysMessage() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/product_barcodes",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"UNIQUE constraint failed"}""",
            method = HttpMethod.Post,
        )
        val viewModel = viewModel()
        viewModel.open()
        advanceUntilIdle()

        viewModel.dialog!!.barcode.setTextAndPlaceCursorAtEnd("4006381333931")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(ChangeError.Failed("UNIQUE constraint failed"), viewModel.dialog?.error)
    }
}
