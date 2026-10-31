package com.patrickzedler.grocy.core.database

import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class RecipeDaoTest {

    private val database = createInMemoryGrocyDatabase()
    private val dao = database.recipeDao()

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun recipesKeepEveryPropertyThroughTheDatabase() = runTest {
        val recipe = Recipe(
            id = 3,
            name = "Pancakes",
            description = "<p>Mix</p>",
            pictureFileName = "pancakes.jpg",
            baseServings = 4.0,
            desiredServings = 2.0,
            notCheckShoppingList = true,
            productId = 9,
            userfields = mapOf("vegan" to "1"),
        )
        val fulfillment = RecipeFulfillment(3, false, true, 2, 4.5, 2.25, 300.0, 11, "Milk", true)
        val ingredient = RecipeIngredient(
            id = 7,
            recipeId = 3,
            productId = 1,
            amount = 0.5,
            quId = 2,
            note = "Cold",
            onlyCheckSingleUnitInStock = true,
            ingredientGroup = "Dough",
            notCheckStockFulfillment = true,
            variableAmount = "a pinch",
            priceFactor = 2.0,
            roundUp = null,
        )
        val nesting = RecipeNesting(id = 1, recipeId = 3, includesRecipeId = 4, servings = 2.0)

        dao.insertRecipes(listOf(recipe.toEntity()))
        dao.insertFulfillment(listOf(fulfillment.toEntity()))
        dao.insertIngredients(listOf(ingredient.toEntity()))
        dao.insertNestings(listOf(nesting.toEntity()))

        assertEquals(recipe, dao.observeRecipe(3).first()?.toModel())
        assertEquals(fulfillment, dao.observeFulfillment().first().single().toModel())
        assertEquals(ingredient, dao.observeIngredients().first().single().toModel())
        assertEquals(nesting, dao.observeNestings().first().single().toModel())
    }

    @Test
    fun resolvedIngredientsAreKeptPerRecipeAndGroupedLikeGrocy() = runTest {
        fun resolved(recipeId: Int, ingredientId: Int, group: String?) = ResolvedRecipeIngredient(
            recipeId = recipeId,
            ingredientId = ingredientId,
            productId = 1,
            recipeAmount = 2.0,
            stockAmount = 1.0,
            needFulfilled = false,
            missingAmount = 1.0,
            amountOnShoppingList = 0.0,
            needFulfilledWithShoppingList = false,
            quId = 2,
            costs = 1.5,
            isNested = recipeId != 3,
            ingredientGroup = group,
            productGroup = null,
            childRecipeId = 3,
            note = null,
            variableAmount = null,
            onlyCheckSingleUnitInStock = false,
            calories = 120.0,
            productActive = true,
            dueScore = 1,
            productIdEffective = 1,
        )
        // The same ingredient resolved for its own recipe and for one including it
        val own = resolved(3, 7, "Topping")
        val nested = resolved(5, 7, "Topping")
        val first = resolved(3, 8, null)

        dao.insertResolvedIngredients(listOf(own, nested, first).map { it.toEntity() })

        assertEquals(
            listOf(first, own),
            dao.observeResolvedIngredients(3).first().map { it.toModel() },
        )
        assertEquals(listOf(nested), dao.observeResolvedIngredients(5).first().map { it.toModel() })
    }

    @Test
    fun picturesOfRemovedRecipesAreDeleted() = runTest {
        dao.insertRecipes(listOf(Recipe(1, "Soup", pictureFileName = "soup.jpg").toEntity()))
        dao.upsertPicture(RecipePictureEntity("soup.jpg", byteArrayOf(1)))
        dao.upsertPicture(RecipePictureEntity("old.jpg", byteArrayOf(2)))

        dao.deleteUnusedPictures()

        assertContentEquals(byteArrayOf(1), dao.picture("soup.jpg"))
        assertNull(dao.picture("old.jpg"))
    }

    @Test
    fun missingPicturesAreTheOnesNotStored() = runTest {
        dao.insertRecipes(
            listOf(
                Recipe(1, "Soup", pictureFileName = "soup.jpg").toEntity(),
                Recipe(2, "Salad", pictureFileName = "salad.jpg").toEntity(),
                Recipe(3, "Bread").toEntity(),
            ),
        )
        dao.upsertPicture(RecipePictureEntity("soup.jpg", byteArrayOf(1)))

        assertEquals(listOf("salad.jpg"), dao.missingPictures())
    }
}
