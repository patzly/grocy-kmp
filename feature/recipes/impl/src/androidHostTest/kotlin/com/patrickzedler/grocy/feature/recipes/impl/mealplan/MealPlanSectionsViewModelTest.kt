package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class MealPlanSectionsViewModelTest {

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

    private fun TestScope.viewModel() = MealPlanSectionsViewModel(environment.mealPlanRepository)
        .also { backgroundScope.launch { it.sections.collect {} } }

    @Test
    fun theListLeavesGrocysDefaultSectionOut() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf("Breakfast"), viewModel.sections.value?.map { it.name })
    }

    @Test
    fun aNewSectionIsPostedAndTheFormCloses() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.add()
        val state = assertIs<MealPlanSectionsDialog.Edit>(viewModel.dialog).state
        state.name.setTextAndPlaceCursorAtEnd("Dinner")
        state.sortNumber.setTextAndPlaceCursorAtEnd("3")
        state.timeInfo.setTextAndPlaceCursorAtEnd("19:00")
        viewModel.save()
        advanceUntilIdle()

        val post = environment.server
            .requestsTo("/api/objects/meal_plan_sections", HttpMethod.Post)
            .single()
        assertEquals(
            """{"name":"Dinner","sort_number":3,"time_info":"19:00"}""",
            (post.body as TextContent).text,
        )
        assertNull(viewModel.dialog)
    }

    @Test
    fun aSectionWithoutNameIsNotSent() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.add()
        viewModel.save()
        advanceUntilIdle()

        assertIs<MealPlanSectionsDialog.Edit>(viewModel.dialog)
        assertTrue(
            environment.server.requestsTo("/api/objects/meal_plan_sections", HttpMethod.Post)
                .isEmpty(),
        )
    }

    @Test
    fun deletingAsksFirst() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val breakfast = viewModel.sections.value!!.single()

        viewModel.requestDelete(breakfast)
        assertEquals(MealPlanSectionsDialog.Delete(breakfast), viewModel.dialog)
        viewModel.delete(breakfast)
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        assertEquals(
            1,
            environment.server.requestsTo("/api/objects/meal_plan_sections/1", HttpMethod.Delete)
                .size,
        )
    }
}
