package com.patrickzedler.grocy.core.model.recipes

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeStatusTest {

    private fun fulfillment(needFulfilled: Boolean, withShoppingList: Boolean) = RecipeFulfillment(
        recipeId = 1,
        needFulfilled = needFulfilled,
        needFulfilledWithShoppingList = withShoppingList,
        missingProductsCount = 0,
        costs = 0.0,
        costsPerServing = 0.0,
        calories = 0.0,
        dueScore = 0,
        productNames = "",
        pricesIncomplete = false,
    )

    @Test
    fun statusFollowsGrocysColumn() {
        assertEquals(RecipeStatus.ENOUGH_IN_STOCK, fulfillment(true, true).status)
        assertEquals(RecipeStatus.ON_SHOPPING_LIST, fulfillment(false, true).status)
        assertEquals(RecipeStatus.NOT_ENOUGH_IN_STOCK, fulfillment(false, false).status)
    }

    @Test
    fun grocycodeNamesTheRecipe() {
        assertEquals("grcy:r:12", recipeGrocycode(12))
    }
}
