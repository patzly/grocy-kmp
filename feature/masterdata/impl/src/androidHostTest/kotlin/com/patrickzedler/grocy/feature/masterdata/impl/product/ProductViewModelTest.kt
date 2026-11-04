package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.data.productfacts.ProductFactsRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.productfacts.ProductFacts
import com.patrickzedler.grocy.core.network.ProductFactsApi
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
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
    private val environment = RepositoryTestEnvironment(dispatcher)

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

    @Test
    fun aNewProductStartsWithTheNameItWasAskedForAndIsNotChangedYet() = runTest(dispatcher) {
        environment.repository.refresh()
        val viewModel = ProductViewModel(
            productId = null,
            repository = environment.repository,
            decimalSeparator = '.',
            presetName = "Brand new",
            returnsWhenSaved = true,
        )
        advanceUntilIdle()

        val state = assertIs<ProductLoadState.Ready>(viewModel.loadState)
        assertEquals("Brand new", state.form.name.text.toString())
        assertFalse(viewModel.hasUnsavedChanges)
        assertTrue(viewModel.returnsWhenSaved)
    }

    /** A view model for a new product with a database that knows [FACTS_JSON]. */
    private suspend fun TestScope.factsViewModel(
        barcode: String? = "3017620422003",
        sources: Set<ProductFactsSource> = setOf(ProductFactsSource.FOOD),
    ): ProductViewModel {
        environment.repository.refresh()
        return ProductViewModel(
            productId = null,
            repository = environment.repository,
            decimalSeparator = '.',
            barcode = barcode,
            productFacts = ProductFactsRepository(
                ProductFactsApi(factsClient(), "test"),
                enabledSources = { sources },
            ),
            language = "en",
        ).also { advanceUntilIdle() }
    }

    private val factsStatus =
        mutableMapOf("/api/v2/product/3017620422003.json" to HttpStatusCode.OK)

    private fun factsClient() = createGrocyHttpClient(
        MockEngine(
            MockEngineConfig().apply {
                dispatcher = this@ProductViewModelTest.dispatcher
                addHandler { request ->
                    val path = request.url.encodedPath
                    val status = factsStatus[path]
                    when {
                        path.endsWith(".jpg") -> respond(byteArrayOf(1, 2, 3))
                        status == null -> respondError(HttpStatusCode.NotFound)
                        !status.isSuccess() -> respondError(status)
                        else -> respond(
                            FACTS_JSON,
                            headers = headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                        )
                    }
                }
            },
        ),
    )

    @Test
    fun aScannedCodeFillsInTheNewProduct() = runTest(dispatcher) {
        val viewModel = factsViewModel()

        val form = viewModel.ready.form
        assertEquals("Nutella", form.name.text.toString())
        assertEquals(
            "Hazelnut spread\nFerrero · 400 g\nSugar\n100 g: FAT 30.9 g",
            form.description.text.toString(),
        )
        assertEquals(
            ProductFactsState.Filled("Open Food Facts"),
            viewModel.factsState,
        )
        val picture = assertIs<PictureChange.Replace>(viewModel.pictureChange)
        assertEquals("3017620422003.jpg", picture.fileName)
        assertTrue(picture.bytes.contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun energyIsPerStockUnitAndFollowsItUntilTheUserTypesTheirOwn() = runTest(dispatcher) {
        val viewModel = factsViewModel()
        val form = viewModel.ready.form
        val gram = viewModel.options.value!!.quantityUnits.first()
        val perPackage = "2156"
        form.changeStockUnit(null)
        advanceUntilIdle()
        assertEquals(perPackage, form.calories.text.toString())

        form.calories.setTextAndPlaceCursorAtEnd("7")
        form.changeStockUnit(gram.id)
        advanceUntilIdle()

        assertEquals("7", form.calories.text.toString())
    }

    @Test
    fun groupAndStoreAreOnlyFilledInWhereOneOfTheUsersHasTheName() = runTest(dispatcher) {
        val viewModel = factsViewModel()
        val form = viewModel.ready.form
        assertEquals(1, form.productGroupId)
        assertEquals(1, form.storeId)
        form.productGroupId = null
        form.storeId = null

        ProductFacts(
            source = ProductFactsSource.FOOD,
            barcode = "3017620422003",
            categories = listOf("Spreads"),
            stores = listOf("Corner shop"),
        ).fillInto(form, viewModel.options.value!!, ProductFactsTexts(), 2, '.')

        assertNull(form.productGroupId)
        assertNull(form.storeId)
    }

    @Test
    fun whatTheUserEnteredStays() = runTest(dispatcher) {
        val viewModel = factsViewModel(barcode = null)
        val form = viewModel.ready.form
        form.name.setTextAndPlaceCursorAtEnd("Mine")

        viewModel.lookUpFacts("3017620422003")
        advanceUntilIdle()

        assertEquals("Mine", form.name.text.toString())
        assertTrue(form.description.text.isNotEmpty())
    }

    @Test
    fun anUnknownBarcodeSaysSoAndKeepsTheForm() = runTest(dispatcher) {
        factsStatus.clear()
        val viewModel = factsViewModel(barcode = null)

        viewModel.events.test {
            viewModel.lookUpFacts("4000000000000")
            advanceUntilIdle()

            assertEquals(ProductEvent.FactsNotFound("4000000000000"), awaitItem())
        }
        assertEquals("", viewModel.ready.form.name.text.toString())
        assertNull(viewModel.factsState)
    }

    @Test
    fun anUnreachableDatabaseSaysSo() = runTest(dispatcher) {
        factsStatus["/api/v2/product/4000000000000.json"] = HttpStatusCode.ServiceUnavailable
        val viewModel = factsViewModel(barcode = null)

        viewModel.events.test {
            viewModel.lookUpFacts("4000000000000")
            advanceUntilIdle()

            assertEquals(ProductEvent.FactsFailed, awaitItem())
        }
    }

    @Test
    fun withoutEnabledDatabasesNothingIsAskedOrOffered() = runTest(dispatcher) {
        val viewModel = factsViewModel(sources = emptySet())

        assertFalse(viewModel.canLookUpFacts)
        assertEquals("", viewModel.ready.form.name.text.toString())
        assertEquals("3017620422003", viewModel.pendingBarcode)
    }

    @Test
    fun theScannedCodeBecomesTheNewProductsBarcode() = runTest(dispatcher) {
        val viewModel = factsViewModel(sources = emptySet())
        val form = viewModel.ready.form
        form.name.setTextAndPlaceCursorAtEnd("Created")
        form.locationId = viewModel.options.value!!.locations.first().id
        val unit = viewModel.options.value!!.quantityUnits.first().id
        form.changeStockUnit(unit)

        assertTrue(viewModel.save())
        advanceUntilIdle()

        val body = environment.server
            .requestsTo("/api/objects/product_barcodes", HttpMethod.Post)
            .single().body as TextContent
        assertTrue("3017620422003" in body.text)
        assertNull(viewModel.pendingBarcode)
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
        val path = "/api/files/productpictures/eDFicmVhZC5qcGc="
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

    private companion object {
        val FACTS_JSON = """
            {"status":1,"product":{"product_name":"Nutella","generic_name":"Hazelnut spread",
            "brands":"Ferrero","quantity":"400 g","product_quantity":400,
            "product_quantity_unit":"g","ingredients_text":"Sugar",
            "categories":"Snacks, dairy","stores":"Corner shop,supermarket",
            "image_front_url":"https://images.openfoodfacts.org/front.jpg",
            "nutriments":{"energy-kcal_100g":539,"fat_100g":30.9}}}
        """.trimIndent()
    }
}
