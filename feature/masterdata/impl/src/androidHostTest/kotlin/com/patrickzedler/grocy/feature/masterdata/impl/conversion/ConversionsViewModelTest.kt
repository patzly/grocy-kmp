package com.patrickzedler.grocy.feature.masterdata.impl.conversion

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import io.ktor.http.HttpMethod
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

class ConversionsViewModelTest {

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

    private suspend fun TestScope.viewModel(owner: ConversionOwner): ConversionsViewModel {
        environment.repository.refresh()
        return ConversionsViewModel(environment.repository, '.', "en").also {
            backgroundScope.launch { it.items.collect {} }
            backgroundScope.launch { it.resolvedGroups.collect {} }
            it.showOwner(owner)
            advanceUntilIdle()
        }
    }

    @Test
    fun unitShowsItsDefaultConversion() = runTest(dispatcher) {
        val viewModel = viewModel(ConversionOwner.Unit(2))

        assertEquals(
            ConversionMeaning("Pack", "6", "Pieces"),
            viewModel.items.value.single().meaning,
        )
    }

    @Test
    fun productWithoutOverridesShowsNone() = runTest(dispatcher) {
        val viewModel = viewModel(ConversionOwner.Product(1))

        assertTrue(viewModel.items.value.isEmpty())
        // What Grocy derives for it from the default conversion
        assertEquals(listOf("Pack", "Piece"), viewModel.resolvedGroups.value.map { it.from })
    }

    @Test
    fun newProductOverrideStartsFromTheStockUnit() = runTest(dispatcher) {
        val viewModel = viewModel(ConversionOwner.Product(1))

        viewModel.open()
        advanceUntilIdle()

        val dialog = assertNotNull(viewModel.dialog)
        assertEquals(1, dialog.fromQuId)
        assertEquals("1", dialog.factor.text.toString())
        assertTrue(dialog.isProductOverride)
    }

    @Test
    fun defaultConversionStartsFromItsFixedUnit() = runTest(dispatcher) {
        val viewModel = viewModel(ConversionOwner.Unit(2))

        viewModel.open()
        advanceUntilIdle()

        assertEquals(2, viewModel.dialog?.fromQuId)
        assertEquals(true, viewModel.dialog?.isFromFixed)
    }

    @Test
    fun unitCannotBeConvertedIntoItself() = runTest(dispatcher) {
        val viewModel = viewModel(ConversionOwner.Unit(2))
        viewModel.open()
        advanceUntilIdle()

        viewModel.dialog!!.toQuId = 2
        viewModel.save()
        advanceUntilIdle()

        assertEquals(ConversionError.SameUnit, viewModel.dialog?.errors?.get(ConversionField.TO))
    }

    @Test
    fun overrideIsSavedForTheProduct() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/quantity_unit_conversions",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        environment.server.respond(
            "/api/objects/quantity_unit_conversions/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"from_qu_id":2,"to_qu_id":1,""" +
                """"factor":10,"product_id":1}""",
        )
        val viewModel = viewModel(ConversionOwner.Product(1))
        viewModel.open()
        advanceUntilIdle()

        viewModel.dialog!!.fromQuId = 2
        viewModel.dialog!!.toQuId = 1
        viewModel.dialog!!.factor.setTextAndPlaceCursorAtEnd("10")
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        val post = environment.server
            .requestsTo("/api/objects/quantity_unit_conversions", HttpMethod.Post).single()
        assertTrue("\"product_id\":1" in (post.body as TextContent).text)
        assertEquals("10", viewModel.items.value.single().meaning.amount)
    }

    @Test
    fun offlineSaveKeepsTheDialog() = runTest(dispatcher) {
        val viewModel = viewModel(ConversionOwner.Unit(2))
        viewModel.open()
        advanceUntilIdle()
        environment.server.unreachable = true

        viewModel.dialog!!.toQuId = 1
        viewModel.save()
        advanceUntilIdle()

        assertEquals(ChangeError.Offline, viewModel.dialog?.error)
    }
}
