package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Grocy's recipes, their ingredients, inclusions and the actions of its recipe page. */
class GrocyApiRecipesTest {

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

    private fun MockRequestHandleScope.noContent() =
        respond("", HttpStatusCode.NoContent, headersOf())

    private val HttpRequestData.json
        get() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun recipesAreTheNormalOnesWithGrocysDefaults() = runTest {
        val api = api {
            json(
                """[{"id":3,"name":"Pancakes","base_servings":"4","desired_servings":"2",""" +
                    """"not_check_shoppinglist":"1","product_id":"5","description":"",""" +
                    """"picture_file_name":"","userfields":{"vegan":"1"}},""" +
                    """{"id":4,"name":"Toast"}]""",
            )
        }

        val recipes = api.recipeObjects(CONNECTION)

        assertEquals("type=normal", requests.single().url.parameters["query[]"])
        assertEquals(
            Recipe(
                id = 3,
                name = "Pancakes",
                baseServings = 4.0,
                desiredServings = 2.0,
                notCheckShoppingList = true,
                productId = 5,
                userfields = mapOf("vegan" to "1"),
            ),
            recipes[0],
        )
        assertEquals(Recipe(id = 4, name = "Toast"), recipes[1])
    }

    @Test
    fun savedRecipeSendsGrocysFormFieldsAsNormalRecipe() = runTest {
        val api = api { json("""{"created_object_id":12}""") }
        val recipe = Recipe(
            id = GrocyApi.NEW_OBJECT_ID,
            name = "Soup",
            baseServings = 2.0,
            desiredServings = 3.0,
        )

        val id = api.saveRecipe(CONNECTION, recipe)

        assertEquals(12, id)
        val body = requests.single().json
        assertEquals("Soup", body.getValue("name").jsonPrimitive.content)
        assertEquals(2.0, body.getValue("base_servings").jsonPrimitive.double)
        assertEquals(0, body.getValue("not_check_shoppinglist").jsonPrimitive.int)
        assertEquals("normal", body.getValue("type").jsonPrimitive.content)
        // Grocy's form clears them
        assertEquals(JsonNull, body.getValue("product_id"))
        assertEquals(JsonNull, body.getValue("picture_file_name"))
    }

    @Test
    fun desiredServingsAreSentAlone() = runTest {
        val api = api { noContent() }

        api.saveDesiredServings(CONNECTION, recipeId = 3, servings = 6.0)

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/objects/recipes/3", request.url.encodedPath)
        assertEquals(setOf("desired_servings"), request.json.keys)
    }

    @Test
    fun fulfillmentReadsGrocysNumbersInEveryForm() = runTest {
        val api = api {
            json(
                """[{"recipe_id":"3","need_fulfilled":"0",""" +
                    """"need_fulfilled_with_shopping_list":1,"missing_products_count":"2",""" +
                    """"costs":"4.5","costs_per_serving":2.25,"calories":"300",""" +
                    """"due_score":"11","product_names_comma_separated":"Milk,Eggs",""" +
                    """"prices_incomplete":"1"}]""",
            )
        }

        val fulfillment = api.recipesFulfillment(CONNECTION).single()

        assertEquals("/api/recipes/fulfillment", requests.single().url.encodedPath)
        assertEquals(RecipeStatus.ON_SHOPPING_LIST, fulfillment.status)
        assertEquals(2, fulfillment.missingProductsCount)
        assertEquals(2.25, fulfillment.costsPerServing)
        assertEquals(11, fulfillment.dueScore)
        assertEquals("Milk,Eggs", fulfillment.productNames)
        assertTrue(fulfillment.pricesIncomplete)
    }

    @Test
    fun ingredientSendsRoundUpOnlyWhereTheServerKnowsIt() = runTest {
        val api = api { noContent() }
        val ingredient = RecipeIngredient(
            id = 4,
            recipeId = 3,
            productId = 1,
            amount = 2.0,
            quId = 1,
            variableAmount = "a pinch",
        )

        api.saveRecipeIngredient(CONNECTION, ingredient)
        api.saveRecipeIngredient(CONNECTION, ingredient.copy(roundUp = true))

        assertFalse("round_up" in requests[0].json)
        assertEquals(1, requests[1].json.getValue("round_up").jsonPrimitive.int)
        assertEquals("a pinch", requests[0].json.getValue("variable_amount").jsonPrimitive.content)
        assertEquals("/api/objects/recipes_pos/4", requests[0].url.encodedPath)
    }

    @Test
    fun ingredientsReadTheirOptions() = runTest {
        val api = api {
            json(
                """[{"id":4,"recipe_id":"3","product_id":"1","amount":"0.5","qu_id":null,""" +
                    """"only_check_single_unit_in_stock":"1","ingredient_group":"",""" +
                    """"not_check_stock_fulfillment":1,"price_factor":"2"}]""",
            )
        }

        val ingredient = api.recipeIngredients(CONNECTION).single()

        assertEquals(0.5, ingredient.amount)
        assertTrue(ingredient.onlyCheckSingleUnitInStock)
        assertNull(ingredient.ingredientGroup)
        assertEquals(2.0, ingredient.priceFactor)
        // An older server without the column
        assertNull(ingredient.roundUp)
    }

    @Test
    fun nestingsAreSavedWithTheirServings() = runTest {
        val api = api { json("""{"created_object_id":7}""") }

        val id = api.saveRecipeNesting(
            CONNECTION,
            RecipeNesting(GrocyApi.NEW_OBJECT_ID, recipeId = 3, includesRecipeId = 4, 2.0),
        )

        assertEquals(7, id)
        val body = requests.single().json
        assertEquals(4, body.getValue("includes_recipe_id").jsonPrimitive.int)
        assertEquals(2.0, body.getValue("servings").jsonPrimitive.double)
    }

    @Test
    fun resolvedIngredientsAreThoseOfNormalRecipes() = runTest {
        val api = api {
            json(
                """[{"recipe_id":3,"recipe_pos_id":"4","product_id":"1",""" +
                    """"recipe_amount":"1.5","stock_amount":"3","need_fulfilled":"1",""" +
                    """"missing_amount":"0","amount_on_shopping_list":"0",""" +
                    """"need_fulfilled_with_shopping_list":"1","qu_id":"2","costs":"0.75",""" +
                    """"is_nested_recipe_pos":"1","child_recipe_id":"5",""" +
                    """"recipe_variable_amount":"a pinch","due_score":"20",""" +
                    """"product_id_effective":"6","product_active":"0"}]""",
            )
        }

        val ingredient = api.resolvedRecipeIngredients(CONNECTION).single()

        assertEquals("recipe_type=normal", requests.single().url.parameters["query[]"])
        assertEquals(4, ingredient.ingredientId)
        assertEquals(1.5, ingredient.recipeAmount)
        assertTrue(ingredient.isNested)
        assertEquals(5, ingredient.childRecipeId)
        assertEquals("a pinch", ingredient.variableAmount)
        assertEquals(20, ingredient.dueScore)
        assertEquals(6, ingredient.productIdEffective)
        assertFalse(ingredient.productActive)
    }

    @Test
    fun copyReturnsTheCopysId() = runTest {
        val api = api { json("""{"created_object_id":8}""") }

        assertEquals(8, api.copyRecipe(CONNECTION, 3))
        assertEquals("/api/recipes/3/copy", requests.single().url.encodedPath)
    }

    @Test
    fun missingProductsLeaveOutTheExcludedOnes() = runTest {
        val api = api { noContent() }

        api.addMissingRecipeProductsToShoppingList(CONNECTION, 3, listOf(1, 2))

        val request = requests.single()
        assertEquals(
            "/api/recipes/3/add-not-fulfilled-products-to-shoppinglist",
            request.url.encodedPath,
        )
        assertEquals(
            listOf(1, 2),
            request.json.getValue("excludedProductIds").jsonArray.map { it.jsonPrimitive.int },
        )
    }

    @Test
    fun mealPlanEntryIsARecipeOfASection() = runTest {
        val api = api { json("""{"created_object_id":1}""") }

        api.addRecipeToMealPlan(CONNECTION, 3, "2026-11-02", servings = 2.0, sectionId = -1)

        val body = requests.single().json
        assertEquals("/api/objects/meal_plan", requests.single().url.encodedPath)
        assertEquals("recipe", body.getValue("type").jsonPrimitive.content)
        assertEquals("2026-11-02", body.getValue("day").jsonPrimitive.content)
        assertEquals(3, body.getValue("recipe_id").jsonPrimitive.int)
        assertEquals(2.0, body.getValue("recipe_servings").jsonPrimitive.double)
        assertEquals(-1, body.getValue("section_id").jsonPrimitive.int)
    }

    @Test
    fun recipePicturesComeFromTheirOwnGroup() = runTest {
        val api = api { respond(byteArrayOf(1, 2), HttpStatusCode.OK, headersOf()) }

        api.recipePicture(CONNECTION, "ab.jpg", maxWidth = 400)

        val url = requests.single().url
        // "ab.jpg" in Base64
        assertEquals("/api/files/recipepictures/YWIuanBn", url.encodedPath)
        assertEquals("400", url.parameters["best_fit_width"])
    }

    private companion object {
        val CONNECTION = ServerConnection.ApiKey("https://grocy.test", "key")
    }
}
