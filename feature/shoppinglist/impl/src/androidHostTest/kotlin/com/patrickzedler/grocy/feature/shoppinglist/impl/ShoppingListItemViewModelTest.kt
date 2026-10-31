package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormEvent
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.testing.MasterDataJson
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ShoppingListItemViewModelTest {

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

    private suspend fun TestScope.viewModel(
        id: Int? = null,
        listId: Int? = null,
        productId: Int? = null,
    ): ShoppingListItemViewModel {
        environment.repository.refresh()
        environment.shoppingListRepository.refresh()
        return ShoppingListItemViewModel(
            id = id,
            presetListId = listId,
            presetProductId = productId,
            masterData = environment.repository,
            shoppingLists = environment.shoppingListRepository,
            decimalSeparator = '.',
        ).also { advanceUntilIdle() }
    }

    private val ShoppingListItemViewModel.form
        get() = assertIs<ObjectFormLoad.Ready<ShoppingListItemFormState>>(load).form

    private fun createdBody() = environment.server
        .requestsTo("/api/objects/shopping_list", HttpMethod.Post).single()
        .let { Json.parseToJsonElement((it.body as TextContent).text).jsonObject }

    @Test
    fun editedItemShowsItsAmountInItsUnit() = runTest(dispatcher) {
        val viewModel = viewModel(id = 5)

        val form = viewModel.form
        assertEquals(1, form.productId)
        assertEquals(2, form.inputs.quId)
        // 6 pieces are one pack
        assertEquals("1", form.inputs.amount.text.toString())
        assertEquals("Low fat", form.inputs.note.text.toString())
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun newItemStartsOnTheNamedListWithOne() = runTest(dispatcher) {
        val viewModel = viewModel(listId = 2)

        val form = viewModel.form
        assertEquals(2, form.inputs.listId)
        assertEquals("1", form.inputs.amount.text.toString())
        assertNull(form.productId)
        assertTrue(form.inputs.units.isEmpty())
    }

    @Test
    fun choosingAProductStartsWithItsPurchaseUnit() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.form.selectProduct(1)

        assertEquals(2, viewModel.form.inputs.quId)
        assertEquals("Piece", viewModel.form.inputs.stockUnit?.name)
    }

    @Test
    fun productOrNoteIsRequired() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertFalse(viewModel.save())

        assertTrue(ObjectField.PRODUCT in viewModel.errors)
        assertTrue(ObjectField.NOTE in viewModel.errors)
    }

    @Test
    fun createdItemIsSentInTheStockUnitAndClosesTheForm() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/shopping_list/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"product_id":1,"amount":12,"qu_id":2}""",
        )
        val viewModel = viewModel(productId = 1)

        viewModel.events.test {
            viewModel.form.inputs.amount.setTextAndPlaceCursorAtEnd("2")
            assertTrue(viewModel.save())
            advanceUntilIdle()

            // Like Grocy's dialog a created item closes the form too
            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
        val body = createdBody()
        assertEquals(12.0, body.getValue("amount").jsonPrimitive.double, 1e-6)
        assertEquals(2, body.getValue("qu_id").jsonPrimitive.int)
        // Grocy's first list by name
        assertEquals(2, body.getValue("shopping_list_id").jsonPrimitive.int)
    }

    @Test
    fun noteOnlyItemHasNoUnit() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/shopping_list/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"note":"Nails","amount":3}""",
        )
        val viewModel = viewModel()

        viewModel.form.inputs.note.setTextAndPlaceCursorAtEnd("Nails")
        viewModel.form.inputs.amount.setTextAndPlaceCursorAtEnd("3")
        viewModel.save()
        advanceUntilIdle()

        val body = createdBody()
        assertEquals(JsonNull, body.getValue("product_id"))
        assertEquals(JsonNull, body.getValue("qu_id"))
        assertEquals(3.0, body.getValue("amount").jsonPrimitive.double)
    }

    @Test
    fun savingOfflineReportsIt() = runTest(dispatcher) {
        val viewModel = viewModel(id = 5)
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.form.inputs.note.setTextAndPlaceCursorAtEnd("Skimmed")
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Offline, awaitItem())
        }
    }
}
