package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.data.settings.RecipesOptionsRepository
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeGrouping
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.recipes.RecipesView
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class RecipesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val settingsStore = FakeSettingsStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(search: String? = null) = RecipesViewModel(
        recipes = environment.recipesRepository,
        masterData = environment.repository,
        // Read right away like at app start
        optionsRepository = RecipesOptionsRepository(
            settingsStore,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ),
        search = search,
        computeDispatcher = dispatcher,
    ).also { backgroundScope.launch { it.state.collect {} } }

    private val RecipesViewModel.ids
        get() = state.value!!.groups.flatMap { group -> group.items.map { it.id } }

    @Test
    fun openingDownloadsAndShowsTheRecipesByName() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.ids)
        assertTrue(viewModel.refresh.value.loadedOnce)
        val pancakes = viewModel.state.value!!.groups.single().items.first()
        assertEquals(RecipeStatus.ON_SHOPPING_LIST, pancakes.status)
    }

    @Test
    fun searchOfTheStockOverviewShowsTheSearchedRecipes() = runTest(dispatcher) {
        val viewModel = viewModel(search = "Bread")

        advanceUntilIdle()

        // Pancakes need Bread
        assertTrue(viewModel.isSearchVisible)
        assertEquals(listOf(1), viewModel.ids)
    }

    @Test
    fun clearFilterResetsStatusAndSearchButNotTheOptions() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setStatus(RecipeStatus.ENOUGH_IN_STOCK)
        viewModel.searchState.setTextAndPlaceCursorAtEnd("Soup")
        viewModel.setGrouping(RecipeGrouping.DUE_SCORE)
        advanceUntilIdle()

        viewModel.clearFilter()
        advanceUntilIdle()

        assertFalse(viewModel.state.value!!.filter.isActive)
        assertFalse(viewModel.isSearchVisible)
        assertEquals(RecipeGrouping.DUE_SCORE, viewModel.state.value!!.grouping)
    }

    @Test
    fun viewAndOptionsAreKept() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setView(RecipesView.GALLERY)
        viewModel.setSort(RecipeField.DUE_SCORE, ascending = false)
        advanceUntilIdle()

        assertEquals(RecipesView.GALLERY, settingsStore.recipesOptions.value.view)
        assertEquals(RecipeField.DUE_SCORE, viewModel.state.value!!.sort.field)
    }

    @Test
    fun hidingTheSortedFieldSortsByNameAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setSort(RecipeField.DUE_SCORE, ascending = false)
        advanceUntilIdle()

        viewModel.setFieldShown(RecipeField.DUE_SCORE, shown = false)
        advanceUntilIdle()

        assertEquals(
            RecipesOptions(fields = setOf(RecipeField.REQUIREMENTS_FULFILLED)),
            settingsStore.recipesOptions.value,
        )
        assertTrue(viewModel.state.value!!.isDefaultSort)
    }

    @Test
    fun statusFilterNeedsTheStockFeature() = runTest(dispatcher) {
        environment.server.respond("/api/system/config", body = """{"FEATURE_FLAG_STOCK":"0"}""")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setStatus(RecipeStatus.ENOUGH_IN_STOCK)
        advanceUntilIdle()

        assertFalse(viewModel.state.value!!.filter.isActive)
        assertEquals(listOf(1, 2), viewModel.ids)
        assertFalse(RecipeField.REQUIREMENTS_FULFILLED in viewModel.state.value!!.fields)
    }
}
