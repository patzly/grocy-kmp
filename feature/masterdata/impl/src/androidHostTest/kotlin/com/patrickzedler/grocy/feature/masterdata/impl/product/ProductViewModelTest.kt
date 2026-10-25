package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.encodeBase64
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

class ProductViewModelTest {

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

    private suspend fun TestScope.viewModel(productId: Int?): ProductViewModel {
        environment.repository.refresh()
        return ProductViewModel(productId, environment.repository, decimalSeparator = '.').also {
            advanceUntilIdle()
        }
    }

    private val ProductViewModel.ready: ProductLoadState.Ready
        get() = assertIs<ProductLoadState.Ready>(loadState)

    @Test
    fun newProductStartsWithThePresets() = runTest(dispatcher) {
        val viewModel = viewModel(productId = null)

        val form = viewModel.ready.form
        assertTrue(viewModel.isNew)
        assertEquals(2, form.locationId)
        assertEquals(1, form.quIdStock)
        assertEquals(1, form.quIdPrice)
        assertEquals(2, viewModel.ready.decimals)
    }

    @Test
    fun formShowsTheStoredListPictureOffline() = runTest(dispatcher) {
        val path = "/api/files/productpictures/${"x1bread.jpg".encodeBase64()}"
        environment.server.respond(path, body = "jpeg")
        val viewModel = viewModel(productId = 2)
        viewModel.thumbnail()
        environment.server.unreachable = true

        assertEquals("jpeg", viewModel.thumbnail()?.decodeToString())
    }

    @Test
    fun productWithoutPictureHasNoThumbnail() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        assertNull(viewModel.thumbnail())
    }

    @Test
    fun existingProductIsLoadedFromTheDatabase() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        val form = viewModel.ready.form
        assertEquals("Milk", form.name.text.toString())
        assertEquals("2", form.minStockAmount.text.toString())
        assertEquals(2, form.quIdPurchase)
        assertEquals("Acme", viewModel.ready.userfields.single().text.text.toString())
    }

    @Test
    fun unknownProductIsNotFound() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 42)

        assertEquals(ProductLoadState.NotFound, viewModel.loadState)
    }

    @Test
    fun invalidInputIsNotSent() = runTest(dispatcher) {
        val viewModel = viewModel(productId = null)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(FieldError.Required, viewModel.errors[ProductField.NAME])
        val posts = environment.server.requestsTo("/api/objects/products", HttpMethod.Post)
        assertTrue(posts.isEmpty())
    }

    @Test
    fun loadedProductHasNoUnsavedChanges() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun editedFieldIsAnUnsavedChange() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        viewModel.ready.form.active = false

        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun editedUserfieldIsAnUnsavedChange() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        viewModel.ready.userfields.single().text.setTextAndPlaceCursorAtEnd("Other")

        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun pickerTextThatNamesNoOptionIsAnUnsavedChange() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        viewModel.ready.form.setUnmatched(ProductChoice.STORE, unmatched = true)

        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun changedPictureIsAnUnsavedChange() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        viewModel.pictureChange = PictureChange.Delete

        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun createdProductHasNoUnsavedChanges() = runTest(dispatcher) {
        val viewModel = viewModel(productId = null)
        viewModel.ready.form.name.setTextAndPlaceCursorAtEnd("Created")

        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun invalidInputReportsThatNothingIsSaved() = runTest(dispatcher) {
        val viewModel = viewModel(productId = null)

        assertFalse(viewModel.save())
    }

    @Test
    fun pickerTextThatNamesNoOptionIsNotSent() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)
        viewModel.ready.form.setUnmatched(ProductChoice.PARENT, unmatched = true)

        viewModel.save()
        advanceUntilIdle()

        val puts = environment.server.requestsTo("/api/objects/products/1", HttpMethod.Put)
        assertTrue(puts.isEmpty())
    }

    @Test
    fun pickerTextThatMatchesAgainIsSent() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)
        viewModel.ready.form.setUnmatched(ProductChoice.PARENT, unmatched = true)
        viewModel.ready.form.setUnmatched(ProductChoice.PARENT, unmatched = false)

        viewModel.save()
        advanceUntilIdle()

        val puts = environment.server.requestsTo("/api/objects/products/1", HttpMethod.Put)
        assertEquals(1, puts.size)
    }

    @Test
    fun createdProductTurnsTheFormIntoItsEditForm() = runTest(dispatcher) {
        val viewModel = viewModel(productId = null)
        viewModel.ready.form.name.setTextAndPlaceCursorAtEnd("Created")

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ProductEvent.Created, awaitItem())
        }
        assertEquals(MasterDataJson.CREATED_ID, viewModel.productId)
        assertFalse(viewModel.isNew)
    }

    @Test
    fun savedProductClosesTheForm() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)
        environment.server.respond(
            "/api/objects/products/1",
            body = """{"id":1,"name":"Whole milk","qu_id_stock":1}""",
            method = HttpMethod.Get,
        )
        viewModel.ready.form.name.setTextAndPlaceCursorAtEnd("Whole milk")

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ProductEvent.Saved, awaitItem())
        }
        val puts = environment.server.requestsTo("/api/objects/products/1", HttpMethod.Put)
        assertEquals(1, puts.size)
    }

    @Test
    fun savingOfflineReportsIt() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ProductEvent.Offline, awaitItem())
        }
        assertFalse(viewModel.isSaving)
    }

    @Test
    fun rejectedProductShowsGrocysMessage() = runTest(dispatcher) {
        val viewModel = viewModel(productId = null)
        viewModel.ready.form.name.setTextAndPlaceCursorAtEnd("Milk")
        environment.server.respond(
            path = "/api/objects/products",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"UNIQUE constraint failed: products.name"}""",
            method = HttpMethod.Post,
        )

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(
                ProductEvent.Failed("UNIQUE constraint failed: products.name"),
                awaitItem(),
            )
        }
    }

    @Test
    fun parentCandidatesAreActiveProductsWithoutParent() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 2)
        val options = viewModel.options.value!!

        // Old cheese is disabled, Yoghurt has a parent, Bread is the product itself
        assertEquals(listOf("Milk"), viewModel.parentCandidates(options).map { it.name })
    }

    @Test
    fun parentProductsCannotGetAParent() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        assertTrue(viewModel.hasSubProducts(viewModel.options.value!!))
    }

    @Test
    fun scannedCodesFindTheirProduct() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 2)
        val options = viewModel.options.value!!

        assertEquals("Milk", viewModel.productForCode(options, "4006381333931")?.name)
        assertEquals("Yoghurt", viewModel.productForCode(options, "grcy:p:4")?.name)
        assertEquals(null, viewModel.productForCode(options, "unknown"))
    }

    @Test
    fun onlyChangedUserfieldsAreSaved() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)
        environment.server.respond(
            "/api/objects/products/1",
            body = """{"id":1,"name":"Milk","qu_id_stock":1}""",
            method = HttpMethod.Get,
        )
        viewModel.ready.userfields.single().text.setTextAndPlaceCursorAtEnd("Other")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            1,
            environment.server.requestsTo("/api/userfields/products/1", HttpMethod.Put).size,
        )
    }

    @Test
    fun newIdIsUsedForNewProducts() {
        assertEquals(0, MasterDataRepository.NEW_PRODUCT_ID)
    }
}
