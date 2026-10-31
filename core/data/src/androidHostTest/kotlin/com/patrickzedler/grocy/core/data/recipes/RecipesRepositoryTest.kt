package com.patrickzedler.grocy.core.data.recipes

import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class RecipesRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "k") }
    private val repository = RecipesRepository(server.api, database, connection)

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresTheRecipesAndWhatTheServerComputed() = runTest {
        repository.refresh()

        assertEquals(listOf("Pancakes", "Soup"), repository.recipes.first().map { it.name })
        assertEquals(2, repository.ingredients.first().size)
        assertEquals(2, repository.nestings.first().single().recipeId)
        assertEquals(
            listOf(1, 2),
            repository.resolvedIngredients(1).first().map { it.ingredientId },
        )
        // Soup's own copy of the included Pancakes' Milk
        assertTrue(repository.resolvedIngredients(2).first().single().isNested)
    }

    @Test
    fun mealPlanRecipesAreLeftOut() = runTest {
        repository.refresh()

        // Grocy's fulfillment also has the recipes of meal plan entries
        assertEquals(listOf(1, 2), repository.fulfillment.first().map { it.recipeId })
    }

    @Test
    fun recipesStayOfflineWhenTheirRefreshFails() = runTest {
        repository.refresh()
        server.respond("/api/recipes/fulfillment", HttpStatusCode.InternalServerError)

        assertFailsWith<GrocyApiException.HttpError> { repository.refresh() }

        assertEquals(2, repository.recipes.first().size)
        assertEquals(2, repository.fulfillment.first().size)
    }

    @Test
    fun desiredServingsDownloadTheScaledIngredientsAgain() = runTest {
        repository.refresh()
        server.requests.clear()

        repository.setDesiredServings(1, 6.0)

        val put = server.requestsTo("/api/objects/recipes/1", HttpMethod.Put).single()
        assertEquals("""{"desired_servings":6.0}""", (put.body as TextContent).text)
        assertTrue("/api/objects/recipes_pos_resolved" in server.requestedPaths)
    }

    @Test
    fun aFailedDownloadAfterAChangeNeverFailsTheChange() = runTest {
        repository.refresh()
        server.respond("/api/recipes/fulfillment", HttpStatusCode.InternalServerError)

        // Saved on the server; the next refresh gets what it computed
        repository.consumeRecipe(1)

        assertEquals(1, server.requestsTo("/api/recipes/1/consume", HttpMethod.Post).size)
    }

    @Test
    fun deletedRecipeIsGoneEvenWhenTheDownloadAfterwardsFails() = runTest {
        repository.refresh()
        server.respond("/api/recipes/fulfillment", HttpStatusCode.InternalServerError)

        repository.deleteRecipe(2)

        assertEquals(1, server.requestsTo("/api/objects/recipes/2", HttpMethod.Delete).size)
        assertEquals(listOf(1), repository.recipes.first().map { it.id })
    }

    @Test
    fun copyReturnsTheCopy() = runTest {
        assertEquals(MasterDataJson.CREATED_ID, repository.copyRecipe(1))
    }

    @Test
    fun replacedPictureIsUploadedUnderANewNameAndTheOldOneDeleted() = runTest {
        repository.refresh()
        val pancakes = repository.recipe(1).first()!!

        repository.saveRecipe(
            recipe = pancakes,
            picture = PictureChange.Replace(byteArrayOf(7), "Crêpe.JPG"),
        )

        val saved = server.requestsTo("/api/objects/recipes/1", HttpMethod.Put).single()
            .let { (it.body as TextContent).text }
        // Grocy's web frontend cleans the name like this, a random prefix makes it new
        assertTrue(Regex(""""picture_file_name":"[0-9a-z]+crpe.jpg"""").containsMatchIn(saved))
        assertEquals(
            1,
            server.requests.count {
                it.method == HttpMethod.Put &&
                    it.url.encodedPath.startsWith("/api/files/recipepictures/")
            },
        )
        assertEquals(
            1,
            server.requestsTo(
                "/api/files/recipepictures/cGFuY2FrZXMuanBn",
                HttpMethod.Delete,
            ).size,
        )
    }

    @Test
    fun picturesAreStoredForOfflineUse() = runTest {
        server.respond("/api/files/recipepictures/cGFuY2FrZXMuanBn", body = "png")
        repository.refresh()

        repository.downloadMissingPictures()
        server.unreachable = true

        assertEquals("png", repository.picture("pancakes.jpg").decodeToString())
    }

    @Test
    fun mealPlanSectionsComeInTheirOrder() = runTest {
        assertEquals(listOf(-1, 1), repository.mealPlanSections().map { it.id })
    }
}
