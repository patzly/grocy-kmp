package com.patrickzedler.grocy.feature.recipes.impl.form

import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.model.recipes.ImportTexts
import com.patrickzedler.grocy.core.network.RecipeImportApi
import com.patrickzedler.grocy.core.network.RecipeImportFailure
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class RecipeImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private var page = PAGE

    private val texts = ImportTexts(
        ingredients = "Ingredients",
        preparation = "Preparation",
        prepTime = "Preparation time",
        cookTime = "Cooking time",
        totalTime = "Total time",
        nutrition = "Per serving",
        nutrients = emptyMap(),
        category = "Category",
        cuisine = "Cuisine",
        author = "Author",
        source = "Source",
        minutes = { "$it min" },
    )

    private val importer = RecipeImportApi(
        httpClient = createGrocyHttpClient(
            MockEngine(
                MockEngineConfig().apply {
                    // Requests finish in virtual time like the fake Grocy server's
                    dispatcher = this@RecipeImportViewModelTest.dispatcher
                    addHandler { request ->
                        if (request.url.encodedPath.endsWith(".jpg")) {
                            val picture = byteArrayOf(1, 2)
                            respond(picture, HttpStatusCode.OK, typed(ContentType.Image.JPEG))
                        } else {
                            respond(page, HttpStatusCode.OK, typed(ContentType.Text.Html))
                        }
                    }
                },
            ),
        ),
        userAgent = "test",
    )

    private fun typed(type: ContentType) = headersOf(HttpHeaders.ContentType, type.toString())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.importing(): RecipeFormViewModel {
        environment.repository.refresh()
        environment.recipesRepository.refresh()
        val viewModel = RecipeFormViewModel(
            id = null,
            repository = environment.repository,
            recipes = environment.recipesRepository,
            language = "en",
            decimalSeparator = '.',
            importUrl = URL,
            importer = importer,
        )
        advanceUntilIdle()
        viewModel.startImport(texts)
        advanceUntilIdle()
        return viewModel
    }

    private val RecipeFormViewModel.form
        get() = assertIs<ObjectFormLoad.Ready<RecipeFormState>>(load).form

    @Test
    fun theRecipePageFillsTheNewRecipe() = runTest(dispatcher) {
        val viewModel = importing()

        assertEquals(RecipeImportState.Done, viewModel.importState)
        assertEquals("Pancakes deluxe", viewModel.form.name.text.toString())
        assertEquals("2", viewModel.form.baseServings.text.toString())
        val preparation = viewModel.form.preparation.text.toString()
        assertTrue(preparation.startsWith("Ingredients\n• 2 Pieces Milk"))
        assertTrue(preparation.endsWith("Source: $URL"))
        assertIs<PictureChange.Replace>(viewModel.form.picture)
        assertEquals("example.com", viewModel.importHost)
    }

    @Test
    fun ingredientLinesBecomeProposals() = runTest(dispatcher) {
        val viewModel = importing()

        val lines = viewModel.importLines
        assertEquals(listOf("2 Pieces Milk", "1 Pack Bread", "Salt"), lines.map { it.line.text })
        assertEquals(listOf(1, 2, null), lines.map { it.productId })
        assertEquals(listOf(true, false, false), lines.map { it.isReady })
    }

    @Test
    fun aPageWithoutRecipeFailsAndCanBeTriedAgain() = runTest(dispatcher) {
        page = "<html></html>"
        val viewModel = importing()

        assertEquals(RecipeImportState.Failed(RecipeImportFailure.NO_RECIPE), viewModel.importState)

        page = PAGE
        viewModel.startImport(texts)
        advanceUntilIdle()

        assertEquals(RecipeImportState.Done, viewModel.importState)
    }

    @Test
    fun theReadyProposalsAreAddedOnceTheRecipeIsSaved() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/recipes/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"name":"Pancakes deluxe"}""",
        )
        environment.server.respond(
            "/api/objects/recipes_pos/${MasterDataJson.CREATED_ID}",
            body = MasterDataJson.PANCAKES_MILK,
        )
        val viewModel = importing()

        viewModel.addReadyImportLines()
        advanceUntilIdle()
        // Nothing can be added before the recipe exists
        assertEquals(3, viewModel.importLines.size)

        viewModel.save()
        advanceUntilIdle()
        viewModel.addReadyImportLines()
        advanceUntilIdle()

        val posts = environment.server.requestsTo("/api/objects/recipes_pos", HttpMethod.Post)
        assertEquals(1, posts.size)
        assertEquals(listOf("1 Pack Bread", "Salt"), viewModel.importLines.map { it.line.text })
    }

    @Test
    fun anOpenedProposalIsGoneOnceItIsAnIngredient() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/recipes/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"name":"Pancakes deluxe"}""",
        )
        environment.server.respond(
            "/api/objects/recipes_pos/${MasterDataJson.CREATED_ID}",
            body = MasterDataJson.PANCAKES_MILK,
        )
        val viewModel = importing()
        viewModel.save()
        advanceUntilIdle()

        viewModel.openImportLine(key = 1)
        advanceUntilIdle()
        val dialog = viewModel.ingredientDialog!!
        assertEquals(2, dialog.productId)
        assertEquals("1 Pack Bread", dialog.importedText)
        dialog.quId = 1
        viewModel.saveIngredient()
        advanceUntilIdle()

        assertEquals(listOf("2 Pieces Milk", "Salt"), viewModel.importLines.map { it.line.text })
    }

    @Test
    fun aLeftOutLineIsNoProposalAnyMore() = runTest(dispatcher) {
        val viewModel = importing()

        viewModel.removeImportLine(key = 2)

        assertEquals(listOf(0, 1), viewModel.importLines.map { it.key })
    }

    private companion object {
        const val URL = "https://www.example.com/pancakes"
        const val PAGE = """<html><script type="application/ld+json">
            {"@type": "Recipe", "name": "Pancakes deluxe", "recipeYield": "2 servings",
             "recipeIngredient": ["2 Pieces Milk", "1 Pack Bread", "Salt"],
             "recipeInstructions": "Mix.", "image": "https://www.example.com/pancakes.jpg"}
            </script></html>"""
    }
}
