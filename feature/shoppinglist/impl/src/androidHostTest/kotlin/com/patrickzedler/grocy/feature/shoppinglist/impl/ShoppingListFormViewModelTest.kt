package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.forms.ObjectField
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
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ShoppingListFormViewModelTest {

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

    private suspend fun TestScope.viewModel(id: Int?): ShoppingListFormViewModel {
        environment.shoppingListRepository.refresh()
        return ShoppingListFormViewModel(
            id,
            environment.repository,
            environment.shoppingListRepository,
        ).also { advanceUntilIdle() }
    }

    private val ShoppingListFormViewModel.form
        get() = assertIs<ObjectFormLoad.Ready<ShoppingListFormState>>(load).form

    @Test
    fun nameIsRequired() = runTest(dispatcher) {
        val viewModel = viewModel(id = null)

        assertFalse(viewModel.save())

        assertTrue(ObjectField.NAME in viewModel.errors)
    }

    @Test
    fun renamingSendsOnlyTheName() = runTest(dispatcher) {
        val viewModel = viewModel(id = 2)

        viewModel.form.name.setTextAndPlaceCursorAtEnd("Hardware")
        viewModel.save()
        advanceUntilIdle()

        val put = environment.server
            .requestsTo("/api/objects/shopping_lists/2", HttpMethod.Put).single()
        assertEquals("""{"name":"Hardware"}""", (put.body as TextContent).text)
    }

    @Test
    fun createdListIsShownNextLikeInGrocy() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/shopping_lists/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"name":"Market"}""",
        )
        val viewModel = viewModel(id = null)

        viewModel.form.name.setTextAndPlaceCursorAtEnd("Market")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            MasterDataJson.CREATED_ID,
            environment.shoppingListRepository.selectedListId.value,
        )
    }
}
