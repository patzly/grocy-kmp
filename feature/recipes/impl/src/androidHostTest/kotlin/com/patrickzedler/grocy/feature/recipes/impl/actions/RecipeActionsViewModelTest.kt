package com.patrickzedler.grocy.feature.recipes.impl.actions

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class RecipeActionsViewModelTest {

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

    private suspend fun viewModel(): RecipeActionsViewModel {
        environment.repository.refresh()
        environment.recipesRepository.refresh()
        return RecipeActionsViewModel(
            recipes = environment.recipesRepository,
            masterData = environment.repository,
            today = { epochDayOf("2026-11-02")!! },
        )
    }

    @Test
    fun copyOpensTheCopy() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.copy(1)
            advanceUntilIdle()

            assertEquals(RecipeActionEvent.Copied(MasterDataJson.CREATED_ID), awaitItem())
        }
    }

    @Test
    fun deleteAsksNamingTheRecipe() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.requestDelete(2)
        advanceUntilIdle()

        assertEquals(RecipeActionDialog.Delete(2, "Soup"), viewModel.dialog)
        viewModel.events.test {
            viewModel.delete(2)
            advanceUntilIdle()

            assertEquals(RecipeActionEvent.Deleted(2), awaitItem())
        }
        assertNull(viewModel.dialog)
    }

    @Test
    fun mealPlanEntryIsTodaysAndInTheChosenSection() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.openMealPlan(1)
        advanceUntilIdle()
        val state = assertIs<RecipeActionDialog.MealPlan>(viewModel.dialog).state
        assertEquals(listOf(-1, 1), state.sections.map { it.id })

        state.sectionId = 1
        state.servings.setTextAndPlaceCursorAtEnd("2")
        viewModel.events.test {
            viewModel.saveMealPlan()
            advanceUntilIdle()

            assertEquals(RecipeActionEvent.AddedToMealPlan, awaitItem())
        }

        val post = environment.server.requestsTo("/api/objects/meal_plan", HttpMethod.Post).single()
        val body = (post.body as TextContent).text
        assertTrue(""""day":"2026-11-02"""" in body, body)
        assertTrue(""""section_id":1""" in body, body)
        assertNull(viewModel.dialog)
    }

    @Test
    fun offlineMealPlanSaysSoAndStaysOpen() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true
        viewModel.openMealPlan(1)
        advanceUntilIdle()
        val state = assertIs<RecipeActionDialog.MealPlan>(viewModel.dialog).state

        viewModel.saveMealPlan()
        advanceUntilIdle()

        assertEquals(ChangeError.Offline, state.error)
        assertIs<RecipeActionDialog.MealPlan>(viewModel.dialog)
    }

    @Test
    fun printedLabelIsReported() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.print(1)
            advanceUntilIdle()

            assertEquals(RecipeActionEvent.Printed, awaitItem())
        }
    }
}
