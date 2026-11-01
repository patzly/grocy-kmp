package com.patrickzedler.grocy.feature.recipes.impl.recipe

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.testTimeSource

class RecipeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)

    // Saving the servings waits on the scheduler, so they go before the database closes
    private val created = mutableListOf<RecipeViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(id: Int = 1) = RecipeViewModel(
        recipeId = id,
        recipes = environment.recipesRepository,
        masterData = environment.repository,
        shoppingLists = environment.shoppingListRepository,
        language = "en",
        decimalSeparator = '.',
        computeDispatcher = dispatcher,
        timeSource = testTimeSource,
    ).also { created += it }

    @Test
    fun cardShowsTheResolvedIngredientsByGroup() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals("Pancakes", state.recipe.name)
        assertEquals(RecipeStatus.ON_SHOPPING_LIST, state.fulfillment?.status)
        val groups = state.sections.single().groups
        assertEquals(listOf(null, "Topping"), groups.map { it.title })
        assertEquals("2 Pieces Milk", groups[0].lines.single().text)
        assertEquals("a slice Bread", groups[1].lines.single().text)
        assertEquals("2", viewModel.servings.text.toString())
    }

    @Test
    fun cardShowsOnlyWithItsServingsFilled() = runTest(dispatcher) {
        val viewModel = viewModel()
        val servingsWhenShown = mutableListOf<String>()
        // Unconfined, so it sees the servings at the moment the card appears
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.filterNotNull().collect {
                servingsWhenShown += viewModel.servings.text.toString()
            }
        }

        advanceUntilIdle()

        // Its first frame has the servings, their label never jumps up
        assertEquals("2", servingsWhenShown.first())
    }

    @Test
    fun costsChipOpensTheCostsNotes() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.showCosts()
        assertEquals(RecipeDialog.Costs, viewModel.dialog)
        viewModel.dismissDialog()

        assertNull(viewModel.dialog)
    }

    @Test
    fun includingRecipeShowsTheIncludedOneAsSection() = runTest(dispatcher) {
        val viewModel = viewModel(id = 2)

        advanceUntilIdle()

        val sections = viewModel.state.value!!.sections
        assertEquals(listOf("Soup", "Pancakes"), sections.map { it.name })
        assertTrue(sections[0].groups.isEmpty())
    }

    @Test
    fun desiredServingsSaveOnceTypingPauses() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.servings.setTextAndPlaceCursorAtEnd("3")
        Snapshot.sendApplyNotifications()
        advanceTimeBy(100)
        viewModel.servings.setTextAndPlaceCursorAtEnd("4")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        val puts = environment.server.requestsTo("/api/objects/recipes/1", HttpMethod.Put)
        assertEquals(
            listOf("""{"desired_servings":4.0}"""),
            puts.map { (it.body as TextContent).text },
        )
    }

    @Test
    fun savingServingsShowsTheIndicatorUntilTheIngredientsAreScaled() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.servings.setTextAndPlaceCursorAtEnd("4")
        Snapshot.sendApplyNotifications()
        // After the typing pause, within the indicator's minimum duration
        advanceTimeBy(1_000)
        val whileSaving = viewModel.isSavingServings
        advanceUntilIdle()

        assertTrue(whileSaving)
        assertFalse(viewModel.isSavingServings)
    }

    @Test
    fun invalidServingsAreNotSaved() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.servings.setTextAndPlaceCursorAtEnd("0")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        val puts = environment.server.requestsTo("/api/objects/recipes/1", HttpMethod.Put)
        assertTrue(puts.isEmpty())
    }

    @Test
    fun consumingAsksFirstAndTellsTheRecipe() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestConsume()
        assertEquals(RecipeDialog.Consume, viewModel.dialog)
        viewModel.events.test {
            viewModel.consume()
            advanceUntilIdle()

            assertEquals(RecipeEvent.Consumed("Pancakes"), awaitItem())
        }
        assertNull(viewModel.dialog)
        assertEquals(
            1,
            environment.server.requestsTo("/api/recipes/1/consume", HttpMethod.Post).size,
        )
    }

    @Test
    fun missingProductsLeaveOutTheUncheckedOnes() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestAddMissing()
        assertEquals(
            RecipeDialog.AddMissing(listOf(MissingProduct(2, "Bread"))),
            viewModel.dialog,
        )
        viewModel.events.test {
            viewModel.addMissing(excludedProductIds = listOf(2))
            advanceUntilIdle()

            assertEquals(RecipeEvent.MissingAdded, awaitItem())
        }
        val post = environment.server.requestsTo(
            "/api/recipes/1/add-not-fulfilled-products-to-shoppinglist",
            HttpMethod.Post,
        ).single()
        assertEquals("""{"excludedProductIds":[2]}""", (post.body as TextContent).text)
    }

    @Test
    fun offlineConsumingFails() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.consume()
            advanceUntilIdle()

            assertEquals(RecipeEvent.ConsumeFailed(ChangeError.Offline), awaitItem())
        }
    }

    @Test
    fun doneIngredientsAreOnlyRemembered() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setIngredientDone(1, done = true)
        viewModel.setIngredientDone(2, done = true)
        viewModel.setIngredientDone(2, done = false)

        assertEquals(setOf(1), viewModel.doneIngredients)
        assertFalse(environment.server.requests.any { it.method == HttpMethod.Put })
    }

    @Test
    fun thePreparationModeKeepsTheScreenOnUntilSwitchedOff() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.keepsScreenOn)

        viewModel.setKeepScreenOn(false)

        assertFalse(viewModel.keepsScreenOn)
    }
}
