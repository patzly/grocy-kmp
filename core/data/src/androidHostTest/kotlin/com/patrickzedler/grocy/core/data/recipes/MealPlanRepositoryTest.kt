package com.patrickzedler.grocy.core.data.recipes

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipeType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.network.GrocyApi
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MealPlanRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "k") }
    private val recipes = RecipesRepository(server.api, database, connection)
    private val repository = MealPlanRepository(server.api, database, connection, recipes)

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun bodyOf(path: String, method: HttpMethod) =
        server.requestsTo(path, method).map {
            Json.parseToJsonElement((it.body as TextContent).text).jsonObject
        }

    @Test
    fun refreshStoresTheEntriesAndSections() = runTest {
        repository.refresh()

        assertEquals(listOf(1, 2, 3), repository.entries.first().map { it.id })
        assertEquals(
            listOf(MealPlanEntryType.RECIPE, MealPlanEntryType.PRODUCT, MealPlanEntryType.NOTE),
            repository.entries.first().map { it.type },
        )
        // Grocy's default section first, by sort number
        assertEquals(listOf(-1, 1), repository.sections.first().map { it.id })
        assertEquals("7:00", repository.sections.first().last().timeInfo)
    }

    @Test
    fun theRecipesRefreshStoresWhatGrocyComputedForTheMealPlan() = runTest {
        recipes.refresh()

        val stored = repository.mealPlanRecipes.first().associateBy { it.id }
        assertEquals(setOf(-1, -2, -3, -4), stored.keys)
        assertEquals(MealPlanRecipeType.SHADOW, stored.getValue(-3).type)
        assertEquals("2026-11-02#1", stored.getValue(-3).name)
        assertEquals(5.0, stored.getValue(-3).fulfillment?.costs)
        assertEquals(true, stored.getValue(-3).fulfillment?.needFulfilledWithShoppingList)
        assertEquals(7.5, stored.getValue(-1).fulfillment?.costs)
        // The normal recipes stay apart from them
        assertEquals(listOf("Pancakes", "Soup"), recipes.recipes.first().map { it.name })
    }

    @Test
    fun entriesStayOfflineWhenTheirRefreshFails() = runTest {
        repository.refresh()
        server.respond("/api/objects/meal_plan", HttpStatusCode.InternalServerError)

        assertFailsWith<GrocyApiException.HttpError> { repository.refresh() }

        assertEquals(3, repository.entries.first().size)
    }

    @Test
    fun aSavedEntryIsStoredAsTheServerReturnsItAndTheCostsComeAgain() = runTest {
        val entry = MealPlanEntry(
            id = GrocyApi.NEW_OBJECT_ID,
            day = "2026-11-04",
            type = MealPlanEntryType.NOTE,
            note = "Leftovers",
        )
        // The server has it from now on
        server.respond(
            "/api/objects/meal_plan",
            body = "[${MasterDataJson.CREATED_MEAL_PLAN_ENTRY}]",
            method = HttpMethod.Get,
        )

        val id = repository.saveEntry(entry)

        assertEquals(MasterDataJson.CREATED_ID, id)
        assertEquals(1, server.requestsTo("/api/objects/meal_plan", HttpMethod.Post).size)
        assertTrue("/api/recipes/fulfillment" in server.requestedPaths)
        assertTrue(repository.entries.first().any { it.note == "Leftovers" })
    }

    @Test
    fun deletingRemovesTheEntry() = runTest {
        repository.refresh()
        server.respond(
            "/api/objects/meal_plan",
            body = MasterDataJson.MEAL_PLAN.substringBefore(""",{"id":"2"""") + "]",
        )

        repository.deleteEntry(2)

        assertEquals(1, server.requestsTo("/api/objects/meal_plan/2", HttpMethod.Delete).size)
        assertEquals(listOf(1), repository.entries.first().map { it.id })
    }

    @Test
    fun copyingADayCreatesItsEntriesOnTheOtherDayNotDone() = runTest {
        server.respond(
            "/api/objects/meal_plan",
            body = """[{"id":3,"day":"2026-11-03","type":"note","note":"Eat out",""" +
                """"section_id":1,"done":1}]""",
        )

        repository.copyDay(fromDay = "2026-11-03", toDay = "2026-11-10")

        val created = bodyOf("/api/objects/meal_plan", HttpMethod.Post).single()
        assertEquals("2026-11-10", created.getValue("day").jsonPrimitive.content)
        assertEquals("Eat out", created.getValue("note").jsonPrimitive.content)
        assertEquals(1, created.getValue("section_id").jsonPrimitive.int)
        assertEquals(0, created.getValue("done").jsonPrimitive.int)
    }

    @Test
    fun consumingAPlannedRecipeConsumesItsShadowRecipeAndMarksItDone() = runTest {
        repository.consumeRecipe(entryId = 1, shadowRecipeId = -3)

        assertEquals(1, server.requestsTo("/api/recipes/-3/consume", HttpMethod.Post).size)
        val done = bodyOf("/api/objects/meal_plan/1", HttpMethod.Put).single()
        assertEquals(1, done.getValue("done").jsonPrimitive.int)
    }

    @Test
    fun theWeeksMissingProductsGoOnTheShoppingListThroughItsRecipe() = runTest {
        repository.putMissingOnShoppingList(-1)

        val path = "/api/recipes/-1/add-not-fulfilled-products-to-shoppinglist"
        assertEquals(1, server.requestsTo(path, HttpMethod.Post).size)
    }

    @Test
    fun aSavedSectionIsStored() = runTest {
        val section = MealPlanSection(GrocyApi.NEW_OBJECT_ID, "Dinner", 3, "19:00")

        repository.saveSection(section)

        assertEquals(1, server.requestsTo("/api/objects/meal_plan_sections", HttpMethod.Post).size)
        assertEquals("19:00", repository.sections.first().single().timeInfo)
    }

    @Test
    fun aFailedDownloadAfterAChangeNeverFailsTheChange() = runTest {
        server.respond("/api/recipes/fulfillment", HttpStatusCode.InternalServerError)

        repository.setDone(3, done = false)

        assertEquals(1, server.requestsTo("/api/objects/meal_plan/3", HttpMethod.Put).size)
    }
}
