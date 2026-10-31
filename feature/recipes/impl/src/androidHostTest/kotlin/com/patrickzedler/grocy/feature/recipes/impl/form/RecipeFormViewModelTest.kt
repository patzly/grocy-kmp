package com.patrickzedler.grocy.feature.recipes.impl.form

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

class RecipeFormViewModelTest {

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

    private suspend fun TestScope.viewModel(id: Int?): RecipeFormViewModel {
        environment.repository.refresh()
        environment.recipesRepository.refresh()
        return RecipeFormViewModel(
            id = id,
            repository = environment.repository,
            recipes = environment.recipesRepository,
            language = "en",
            decimalSeparator = '.',
        ).also { advanceUntilIdle() }
    }

    private val RecipeFormViewModel.form
        get() = assertIs<ObjectFormLoad.Ready<RecipeFormState>>(load).form

    @Test
    fun formShowsTheRecipeWithItsIngredientsAndInclusions() = runTest(dispatcher) {
        val viewModel = viewModel(id = 1)

        assertEquals("Pancakes", viewModel.form.name.text.toString())
        assertEquals("4", viewModel.form.baseServings.text.toString())
        assertEquals("Mix and fry", viewModel.form.preparation.text.toString())
        assertEquals(
            listOf("4 Pieces Milk", "a slice Bread"),
            viewModel.related.value.ingredients.map { it.text },
        )
        assertTrue(viewModel.related.value.nestings.isEmpty())
    }

    @Test
    fun nameIsRequired() = runTest(dispatcher) {
        val viewModel = viewModel(id = null)

        assertFalse(viewModel.save())

        assertTrue(ObjectField.NAME in viewModel.errors)
    }

    @Test
    fun savingKeepsTheDescriptionsHtmlAndTheDesiredServings() = runTest(dispatcher) {
        val viewModel = viewModel(id = 1)

        viewModel.form.name.setTextAndPlaceCursorAtEnd("Crêpes")
        viewModel.save()
        advanceUntilIdle()

        val put = environment.server.requestsTo("/api/objects/recipes/1", HttpMethod.Put).single()
        val body = (put.body as TextContent).text
        assertTrue(""""name":"Crêpes"""" in body, body)
        assertTrue(""""description":"<p>Mix and fry</p>"""" in body, body)
        assertTrue(""""desired_servings":2.0""" in body, body)
    }

    @Test
    fun createdRecipeBecomesItsEditFormForTheIngredients() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/recipes/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"name":"Toast"}""",
        )
        val viewModel = viewModel(id = null)
        viewModel.form.name.setTextAndPlaceCursorAtEnd("Toast")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(MasterDataJson.CREATED_ID, viewModel.objectId)
        assertFalse(viewModel.isNew)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun newRecipeCannotGetIngredientsYet() = runTest(dispatcher) {
        val viewModel = viewModel(id = null)

        viewModel.openIngredient()
        advanceUntilIdle()

        assertNull(viewModel.ingredientDialog)
    }

    @Test
    fun editedIngredientShowsInItsUnitAndSavesInTheStockUnit() = runTest(dispatcher) {
        val viewModel = viewModel(id = 1)
        viewModel.openIngredient(1)
        advanceUntilIdle()
        val dialog = assertNotNull(viewModel.ingredientDialog)
        assertEquals("4", dialog.amount.text.toString())

        // Milk's packs hold six pieces
        dialog.quId = 2
        dialog.amount.setTextAndPlaceCursorAtEnd("1")
        viewModel.saveIngredient()
        advanceUntilIdle()

        val put = environment.server.requestsTo("/api/objects/recipes_pos/1", HttpMethod.Put)
            .single()
        val body = (put.body as TextContent).text
        assertTrue(""""amount":6.0""" in body, body)
        assertTrue(""""qu_id":2""" in body, body)
        // The server knows round up, its ingredients have it
        assertTrue(""""round_up":0""" in body, body)
        assertNull(viewModel.ingredientDialog)
    }

    @Test
    fun choosingAProductTakesItsStockUnit() = runTest(dispatcher) {
        val viewModel = viewModel(id = 1)
        viewModel.openIngredient()
        advanceUntilIdle()
        val dialog = assertNotNull(viewModel.ingredientDialog)

        dialog.selectProduct(1)

        assertEquals(1, dialog.quId)
        assertEquals(listOf("Pack", "Piece"), dialog.units.map { it.unit.name })
    }

    @Test
    fun includedRecipeCannotBeTheRecipeItself() = runTest(dispatcher) {
        val viewModel = viewModel(id = 1)
        viewModel.openNesting()
        advanceUntilIdle()
        val dialog = assertNotNull(viewModel.nestingDialog)

        assertEquals(listOf(2), dialog.recipes.map { it.id })
        dialog.recipeId = 2
        dialog.servings.setTextAndPlaceCursorAtEnd("2")
        viewModel.saveNesting()
        advanceUntilIdle()

        val post = environment.server.requestsTo("/api/objects/recipes_nestings", HttpMethod.Post)
            .single()
        assertEquals(
            """{"recipe_id":1,"includes_recipe_id":2,"servings":2.0}""",
            (post.body as TextContent).text,
        )
    }

    @Test
    fun ingredientIsRemovedAfterAsking() = runTest(dispatcher) {
        val viewModel = viewModel(id = 1)

        viewModel.requestDelete(RelatedDelete.Ingredient(2, "a slice Bread"))
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.relatedDelete)
        assertNull(viewModel.relatedDeleteError)
        assertEquals(
            1,
            environment.server.requestsTo("/api/objects/recipes_pos/2", HttpMethod.Delete).size,
        )
    }
}
