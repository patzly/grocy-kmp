package com.patrickzedler.grocy.feature.recipes.impl.list

import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeStatusChipsTest {

    @Test
    fun enoughInStockIsOneGreenChip() {
        assertEquals(listOf(StatusChip.ENOUGH), statusChipsOf(RecipeStatus.ENOUGH_IN_STOCK))
    }

    @Test
    fun missingButOnTheShoppingListIsYellowWithTheCartAfterIt() {
        assertEquals(
            listOf(StatusChip.MISSING_ON_LIST, StatusChip.ON_SHOPPING_LIST),
            statusChipsOf(RecipeStatus.ON_SHOPPING_LIST),
        )
    }

    @Test
    fun missingAndNotOnTheShoppingListIsRedAlone() {
        assertEquals(listOf(StatusChip.MISSING), statusChipsOf(RecipeStatus.NOT_ENOUGH_IN_STOCK))
    }

    @Test
    fun anIngredientPartlyOnTheShoppingListShowsTheCartToo() {
        assertEquals(
            listOf(StatusChip.MISSING, StatusChip.ON_SHOPPING_LIST),
            statusChipsOf(RecipeStatus.NOT_ENOUGH_IN_STOCK, isOnShoppingList = true),
        )
    }

    @Test
    fun enoughInStockNeverShowsTheCart() {
        assertEquals(
            listOf(StatusChip.ENOUGH),
            statusChipsOf(RecipeStatus.ENOUGH_IN_STOCK, isOnShoppingList = true),
        )
    }
}
