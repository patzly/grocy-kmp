package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipe
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipeType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Grocy's meal plan, its sections and the recipes Grocy keeps for it. */
class GrocyApiMealPlanTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        GrocyApi(
            httpClient = createGrocyHttpClient(
                MockEngine { request ->
                    requests += request
                    handler(request)
                },
            ),
            ingressSessionSource = { error("No ingress in these tests") },
            apiKeyAsQueryParameter = false,
        )

    private fun MockRequestHandleScope.json(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.json
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun entriesAreReadWithGrocysDefaults() = runTest {
        val api = api {
            json(
                """[{"id":"1","day":"2026-11-02 00:00:00","type":"product","product_id":"4",""" +
                    """"product_amount":"2.5","product_qu_id":"1","section_id":"3",""" +
                    """"done":"1","note":""},""" +
                    """{"id":2,"day":"2026-11-03","recipe_id":5}]""",
            )
        }

        val entries = api.mealPlan(CONNECTION)

        assertEquals("/api/objects/meal_plan", requests.single().url.encodedPath)
        assertEquals(
            MealPlanEntry(
                id = 1,
                day = "2026-11-02",
                type = MealPlanEntryType.PRODUCT,
                productId = 4,
                productAmount = 2.5,
                productQuId = 1,
                sectionId = 3,
                done = true,
            ),
            entries[0],
        )
        // Entries of old versions are recipes in the default section, one serving
        assertEquals(
            MealPlanEntry(id = 2, day = "2026-11-03", type = MealPlanEntryType.RECIPE, recipeId = 5),
            entries[1],
        )
    }

    @Test
    fun aRecipeEntrySendsOnlyTheFieldsOfItsType() = runTest {
        val api = api { json("""{"created_object_id":7}""") }
        val entry = MealPlanEntry(
            id = GrocyApi.NEW_OBJECT_ID,
            day = "2026-11-02",
            type = MealPlanEntryType.RECIPE,
            recipeId = 3,
            recipeServings = 2.0,
            note = "ignored",
            sectionId = 1,
        )

        val id = api.saveMealPlanEntry(CONNECTION, entry)

        val request = requests.single()
        assertEquals(7, id)
        assertEquals(HttpMethod.Post, request.method)
        val body = request.json
        assertEquals("recipe", body.getValue("type").jsonPrimitive.content)
        assertEquals(3, body.getValue("recipe_id").jsonPrimitive.int)
        assertEquals(2.0, body.getValue("recipe_servings").jsonPrimitive.double)
        assertEquals(1, body.getValue("section_id").jsonPrimitive.int)
        assertEquals(0, body.getValue("done").jsonPrimitive.int)
        assertNull(body["note"])
        assertNull(body["product_id"])
    }

    @Test
    fun aProductEntryIsPutWithItsAmountAndUnit() = runTest {
        val api = api { respond("", HttpStatusCode.NoContent, headersOf()) }
        val entry = MealPlanEntry(
            id = 4,
            day = "2026-11-02",
            type = MealPlanEntryType.PRODUCT,
            productId = 1,
            productAmount = 6.0,
            productQuId = 2,
            done = true,
        )

        api.saveMealPlanEntry(CONNECTION, entry)

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/objects/meal_plan/4", request.url.encodedPath)
        val body = request.json
        assertEquals(6.0, body.getValue("product_amount").jsonPrimitive.double)
        assertEquals(2, body.getValue("product_qu_id").jsonPrimitive.int)
        assertEquals(1, body.getValue("done").jsonPrimitive.int)
        assertNull(body["recipe_id"])
    }

    @Test
    fun doneIsPutAlone() = runTest {
        val api = api { respond("", HttpStatusCode.NoContent, headersOf()) }

        api.setMealPlanEntryDone(CONNECTION, 4, done = true)

        val request = requests.single()
        assertEquals("/api/objects/meal_plan/4", request.url.encodedPath)
        assertEquals("""{"done":1}""", (request.body as TextContent).text)
    }

    @Test
    fun aDayIsReadByItsDate() = runTest {
        val api = api {
            json("""[{"id":1,"day":"2026-11-02","type":"note"},{"id":2,"day":"2026-11-03"}]""")
        }

        val entries = api.mealPlanOfDay(CONNECTION, "2026-11-02")

        assertEquals("day=2026-11-02", requests.single().url.parameters["query[]"])
        // Only the day itself, also from a server that ignores the query
        assertEquals(listOf(1), entries.map { it.id })
    }

    @Test
    fun sectionsKeepTheirTime() = runTest {
        val api = api {
            json(
                """[{"id":-1,"name":"","sort_number":-1,"time_info":""},""" +
                    """{"id":1,"name":"Breakfast","sort_number":"1","time_info":"07:00"}]""",
            )
        }

        val sections = api.mealPlanSections(CONNECTION)

        assertNull(sections[0].timeInfo)
        assertEquals(MealPlanSection(1, "Breakfast", 1, "07:00"), sections[1])
    }

    @Test
    fun sectionsAreSavedWithTheirFormsFields() = runTest {
        val api = api { json("""{"created_object_id":9}""") }

        api.saveMealPlanSection(
            CONNECTION,
            MealPlanSection(GrocyApi.NEW_OBJECT_ID, "Dinner", sortNumber = null, timeInfo = null),
        )

        val body = requests.single().json
        assertEquals("Dinner", body.getValue("name").jsonPrimitive.content)
        assertEquals(JsonNull, body.getValue("sort_number"))
        assertEquals("", body.getValue("time_info").jsonPrimitive.content)
    }

    @Test
    fun mealPlanRecipesAreGrocysOwnOnes() = runTest {
        val api = api {
            json(
                """[{"id":-1,"name":"2026-44","type":"mealplan-week"},""" +
                    """{"id":-2,"name":"2026-11-02#1","type":"mealplan-shadow"},""" +
                    """{"id":1,"name":"Pancakes","type":"normal"}]""",
            )
        }

        val recipes = api.mealPlanRecipes(CONNECTION)

        assertEquals("type!=normal", requests.single().url.parameters["query[]"])
        assertEquals(
            listOf(
                MealPlanRecipe(-1, MealPlanRecipeType.WEEK, "2026-44"),
                MealPlanRecipe(-2, MealPlanRecipeType.SHADOW, "2026-11-02#1"),
            ),
            recipes,
        )
    }

    @Test
    fun normalRecipesLeaveGrocysOwnOnesOut() = runTest {
        val api = api {
            json(
                """[{"id":1,"name":"Pancakes","type":"normal"},""" +
                    """{"id":-2,"name":"2026-11-02","type":"mealplan-day"}]""",
            )
        }

        assertEquals(listOf(1), api.recipeObjects(CONNECTION).map { it.id })
    }

    @Test
    fun theWeeksFirstDayIsReadFromTheConfiguration() = runTest {
        val api = api {
            json("""{"CALENDAR_FIRST_DAY_OF_WEEK":"0","MEAL_PLAN_FIRST_DAY_OF_WEEK":"-1"}""")
        }

        val config = api.serverConfig(CONNECTION)

        assertEquals(0, config.calendarFirstDayOfWeek)
        assertEquals(-1, config.mealPlanFirstDayOfWeek)
    }

    @Test
    fun anEmptyFirstDayMeansTheDefault() = runTest {
        val api = api {
            json("""{"CALENDAR_FIRST_DAY_OF_WEEK":"","MEAL_PLAN_FIRST_DAY_OF_WEEK":"9"}""")
        }

        val config = api.serverConfig(CONNECTION)

        assertNull(config.calendarFirstDayOfWeek)
        assertNull(config.mealPlanFirstDayOfWeek)
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.test", "key")
    }
}
