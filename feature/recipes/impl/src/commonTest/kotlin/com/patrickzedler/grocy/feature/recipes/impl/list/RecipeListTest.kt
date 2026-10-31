package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeGrouping
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipeListTest {

    private val pancakes = Recipe(1, "Pancakes", desiredServings = 2.0)
    private val soup = Recipe(2, "soup", desiredServings = 4.0)
    private val bread = Recipe(3, "Bread", desiredServings = 2.0)
    private val recipes = listOf(pancakes, soup, bread)

    private fun fulfillment(
        id: Int,
        status: RecipeStatus,
        missing: Int = 0,
        dueScore: Int = 0,
        products: String = "",
    ) = RecipeFulfillment(
        recipeId = id,
        needFulfilled = status == RecipeStatus.ENOUGH_IN_STOCK,
        needFulfilledWithShoppingList = status != RecipeStatus.NOT_ENOUGH_IN_STOCK,
        missingProductsCount = missing,
        costs = 0.0,
        costsPerServing = 0.0,
        calories = 0.0,
        dueScore = dueScore,
        productNames = products,
        pricesIncomplete = false,
    )

    private val computed = listOf(
        fulfillment(1, RecipeStatus.ON_SHOPPING_LIST, missing = 1, dueScore = 11, "Milk,Eggs"),
        fulfillment(2, RecipeStatus.ENOUGH_IN_STOCK, dueScore = 1, products = "Carrot"),
        fulfillment(3, RecipeStatus.NOT_ENOUGH_IN_STOCK, missing = 3, dueScore = 1),
    )

    private fun build(
        filter: RecipesFilter = RecipesFilter(),
        sort: RecipeSort = DefaultSort,
        grouping: RecipeGrouping = RecipeGrouping.NONE,
        query: String = "",
        fulfillment: List<RecipeFulfillment> = computed,
    ) = buildRecipeList(recipes, fulfillment, filter, sort, grouping, query)

    private fun RecipeListResult.ids() = groups.flatMap { group -> group.items.map { it.id } }

    @Test
    fun recipesAreSortedByNameLikeGrocyIgnoringCase() {
        assertEquals(listOf(3, 1, 2), build().ids())
    }

    @Test
    fun descendingSortReversesTheOrderButNotTheTies() {
        val result = build(sort = RecipeSort(RecipeField.DESIRED_SERVINGS, ascending = false))

        // Bread and Pancakes have two servings each and stay by name
        assertEquals(listOf(2, 3, 1), result.ids())
    }

    @Test
    fun requirementsSortByStatusThenMissingIngredients() {
        val result = build(sort = RecipeSort(RecipeField.REQUIREMENTS_FULFILLED, ascending = true))

        assertEquals(listOf(2, 1, 3), result.ids())
    }

    @Test
    fun dueScoreSortsNumerically() {
        val result = build(sort = RecipeSort(RecipeField.DUE_SCORE, ascending = false))

        assertEquals(listOf(1, 3, 2), result.ids())
    }

    @Test
    fun statusFilterKeepsTheCountsOfEveryStatus() {
        val result = build(filter = RecipesFilter(RecipeStatus.ENOUGH_IN_STOCK))

        assertEquals(listOf(2), result.ids())
        assertEquals(
            mapOf(
                RecipeStatus.ENOUGH_IN_STOCK to 1,
                RecipeStatus.ON_SHOPPING_LIST to 1,
                RecipeStatus.NOT_ENOUGH_IN_STOCK to 1,
            ),
            result.statusCounts,
        )
    }

    @Test
    fun searchFindsRecipesByTheirIngredientsLikeGrocy() {
        assertEquals(listOf(1), build(query = "eggs").ids())
        // With a typo, like the other searches of the app
        assertEquals(listOf(2), build(query = "soop").ids())
    }

    @Test
    fun groupingByStatusFollowsItsOrderWithItemsByTheSort() {
        val result = build(grouping = RecipeGrouping.REQUIREMENTS_FULFILLED)

        assertEquals(
            listOf(
                RecipeGroupTitle.Status(RecipeStatus.ENOUGH_IN_STOCK),
                RecipeGroupTitle.Status(RecipeStatus.ON_SHOPPING_LIST),
                RecipeGroupTitle.Status(RecipeStatus.NOT_ENOUGH_IN_STOCK),
            ),
            result.groups.map { it.title },
        )
    }

    @Test
    fun groupingByServingsOrdersTheGroupsByTheirValue() {
        val result = build(grouping = RecipeGrouping.DESIRED_SERVINGS)

        assertEquals(
            listOf(RecipeGroupTitle.Servings(2.0), RecipeGroupTitle.Servings(4.0)),
            result.groups.map { it.title },
        )
        assertEquals(listOf(3, 1), result.groups.first().items.map { it.id })
    }

    @Test
    fun recipesNotComputedYetAreGroupedLastAndHaveNoStatus() {
        val result = build(
            grouping = RecipeGrouping.DUE_SCORE,
            fulfillment = computed.filter { it.recipeId != 3 },
        )

        assertEquals(RecipeGroupTitle.None, result.groups.last().title)
        assertNull(result.groups.last().items.single().status)
    }

    @Test
    fun withoutGroupingThereIsOneGroupWithoutTitle() {
        val result = build()

        assertNull(result.groups.single().title)
    }

    @Test
    fun hiddenFieldsSortByNameAgain() {
        assertEquals(DefaultSort, effectiveSort(RecipeField.DUE_SCORE, false, shown = emptyList()))
        assertEquals(
            RecipeSort(RecipeField.DUE_SCORE, ascending = false),
            effectiveSort(RecipeField.DUE_SCORE, false, shown = listOf(RecipeField.DUE_SCORE)),
        )
    }

    @Test
    fun requirementsNeedTheStockFeature() {
        val withoutStock = ServerConfig(stock = false)

        assertEquals(
            listOf(RecipeField.DESIRED_SERVINGS, RecipeField.DUE_SCORE),
            availableFields(withoutStock),
        )
        assertEquals(
            listOf(RecipeGrouping.NONE, RecipeGrouping.DESIRED_SERVINGS, RecipeGrouping.DUE_SCORE),
            availableGroupings(withoutStock),
        )
    }

    @Test
    fun galleryFitsCardsToTheWidth() {
        assertEquals(1, galleryColumns(360.dp))
        assertEquals(2, galleryColumns(600.dp))
        assertEquals(3, galleryColumns(900.dp))
        // Never more than four, however wide
        assertEquals(4, galleryColumns(3000.dp))
    }
}
