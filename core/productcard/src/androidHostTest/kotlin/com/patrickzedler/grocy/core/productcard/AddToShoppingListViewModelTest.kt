package com.patrickzedler.grocy.core.productcard

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
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

class AddToShoppingListViewModelTest {

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

    private suspend fun TestScope.openedViewModel(): AddToShoppingListViewModel {
        environment.repository.refresh()
        return AddToShoppingListViewModel(
            environment.repository,
            environment.shoppingListRepository,
        ).also {
            it.open(productId = 1)
            advanceUntilIdle()
        }
    }

    @Test
    fun formStartsWithOneOfThePurchaseUnitOnTheFirstList() = runTest(dispatcher) {
        val viewModel = openedViewModel()

        val dialog = assertNotNull(viewModel.dialog)
        assertEquals("1", dialog.inputs.amount.text.toString())
        assertEquals(2, dialog.inputs.quId)
        assertEquals(1, dialog.inputs.listId)
        assertEquals(listOf("Hardware store", "Shopping list"), dialog.inputs.lists.map { it.name })
    }

    @Test
    fun withoutMultipleListsNoListIsOffered() = runTest(dispatcher) {
        environment.server.respond(
            "/api/system/config",
            body = """{"FEATURE_FLAG_SHOPPINGLIST_MULTIPLE_LISTS":false}""",
        )

        val viewModel = openedViewModel()

        assertTrue(viewModel.dialog!!.inputs.lists.isEmpty())
        assertEquals(1, viewModel.dialog!!.inputs.listId)
    }

    @Test
    fun amountIsSentInTheStockUnitWithTheChosenUnit() = runTest(dispatcher) {
        val viewModel = openedViewModel()

        viewModel.added.test {
            viewModel.dialog!!.inputs.amount.setTextAndPlaceCursorAtEnd("2")
            viewModel.save()
            advanceUntilIdle()

            val added = awaitItem()
            assertEquals("2", added.amount)
            assertEquals("Milk", added.productName)
            assertEquals("Shopping list", added.listName)
        }
        assertNull(viewModel.dialog)
        val body = environment.server
            .requestsTo("/api/stock/shoppinglist/add-product", HttpMethod.Post).single()
            .let { Json.parseToJsonElement((it.body as TextContent).text).jsonObject }
        // Two packs of six pieces
        assertEquals(12.0, body.getValue("product_amount").jsonPrimitive.double, 1e-6)
        assertEquals(2, body.getValue("qu_id").jsonPrimitive.int)
        assertEquals(1, body.getValue("list_id").jsonPrimitive.int)
    }

    @Test
    fun invalidAmountIsNotSent() = runTest(dispatcher) {
        val viewModel = openedViewModel()

        viewModel.dialog!!.inputs.amount.setTextAndPlaceCursorAtEnd("-1")
        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.dialog!!.errors[ObjectField.AMOUNT])
        assertTrue(
            environment.server.requestsTo("/api/stock/shoppinglist/add-product", HttpMethod.Post)
                .isEmpty(),
        )
    }
}
