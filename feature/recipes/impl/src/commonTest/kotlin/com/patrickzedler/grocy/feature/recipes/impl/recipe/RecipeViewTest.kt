package com.patrickzedler.grocy.feature.recipes.impl.recipe

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import com.patrickzedler.grocy.core.model.stock.DueStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipeViewTest {

    private val piece = QuantityUnit(id = 1, name = "Piece", namePlural = "Pieces")
    private val pack = QuantityUnit(id = 2, name = "Pack", namePlural = "Packs")
    private val milk = Product(id = 1, name = "Milk", quIdStock = 1)
    private val yoghurt = Product(id = 2, name = "Yoghurt", quIdStock = 1)
    private val format = IngredientFormat(
        products = listOf(milk, yoghurt),
        units = listOf(piece, pack),
        // One piece is a sixth of a pack
        conversions = listOf(ResolvedQuantityUnitConversion(1, 1, 2, 1.0 / 6)),
        decimals = 2,
        decimalSeparator = '.',
        language = "en",
    )

    private fun row(
        ingredientId: Int,
        recipeId: Int = 1,
        childRecipeId: Int = recipeId,
        group: String? = null,
        productGroup: String? = null,
        quId: Int = 1,
        amount: Double = 2.0,
        status: RecipeStatus = RecipeStatus.ENOUGH_IN_STOCK,
        missing: Double = 0.0,
        variableAmount: String? = null,
        onlyCheckSingleUnit: Boolean = false,
        dueScore: Int = 0,
        productIdEffective: Int = 1,
    ) = ResolvedRecipeIngredient(
        recipeId = recipeId,
        ingredientId = ingredientId,
        productId = 1,
        recipeAmount = amount,
        stockAmount = 3.0,
        needFulfilled = status == RecipeStatus.ENOUGH_IN_STOCK,
        missingAmount = missing,
        amountOnShoppingList = 0.0,
        needFulfilledWithShoppingList = status != RecipeStatus.NOT_ENOUGH_IN_STOCK,
        quId = quId,
        costs = 0.5,
        isNested = childRecipeId != recipeId,
        ingredientGroup = group,
        productGroup = productGroup,
        childRecipeId = childRecipeId,
        note = null,
        variableAmount = variableAmount,
        onlyCheckSingleUnitInStock = onlyCheckSingleUnit,
        calories = 100.0,
        productActive = true,
        dueScore = dueScore,
        productIdEffective = productIdEffective,
    )

    @Test
    fun amountsShowInTheIngredientsUnitThoughGrocyKeepsThemInTheStockUnit() {
        val line = ingredientLine(row(1, quId = 2, amount = 12.0), format, checksStock = true)

        assertEquals("2 Packs Milk", line.text)
        assertEquals("3 Pieces", line.stockAmount)
    }

    @Test
    fun anyAmountInStockKeepsTheAmountAsTyped() {
        val line = ingredientLine(
            row(1, quId = 2, amount = 1.0, onlyCheckSingleUnit = true),
            format,
            checksStock = true,
        )

        assertEquals("1 Pack Milk", line.text)
    }

    @Test
    fun variableAmountReplacesAmountAndUnit() {
        val line = ingredientLine(row(1, variableAmount = "a pinch"), format, checksStock = true)

        assertEquals("a pinch Milk", line.text)
        assertEquals(true, line.isVariableAmount)
    }

    @Test
    fun withoutTheStockFeatureThereIsNoStatus() {
        assertNull(ingredientLine(row(1), format, checksStock = false).status)
    }

    @Test
    fun dueScoresBecomeGrocysHighlighting() {
        fun dueStatus(score: Int) = ingredientLine(row(1, dueScore = score), format, true).dueStatus

        assertEquals(DueStatus.DUE_SOON, dueStatus(1))
        assertEquals(DueStatus.OVERDUE, dueStatus(10))
        assertEquals(DueStatus.EXPIRED, dueStatus(20))
        assertNull(dueStatus(0))
    }

    @Test
    fun subProductInPlaceOfTheParentIsNamed() {
        val line = ingredientLine(row(1, productIdEffective = 2), format, checksStock = true)

        assertEquals(2 to "Yoghurt", line.substitute)
        assertNull(ingredientLine(row(1), format, checksStock = true).substitute)
    }

    @Test
    fun ingredientsAreGroupedUngroupedFirst() {
        val sections = buildRecipeSections(
            recipe = Recipe(1, "Pancakes"),
            recipes = listOf(Recipe(1, "Pancakes")),
            resolved = listOf(row(1, group = "Topping"), row(2), row(3, group = "Dough")),
            format = format,
            groupByProductGroup = false,
            checksStock = true,
        )

        assertEquals(
            listOf(null, "Dough", "Topping"),
            sections.single().groups.map { it.title },
        )
    }

    @Test
    fun productGroupsSubdivideTheGroupsWithGrocysSetting() {
        val sections = buildRecipeSections(
            recipe = Recipe(1, "Pancakes"),
            recipes = emptyList(),
            resolved = listOf(row(1, productGroup = "Dairy"), row(2, group = "Dough")),
            format = format,
            groupByProductGroup = true,
            checksStock = true,
        )

        assertEquals(listOf("Dairy", "Dough"), sections.single().groups.map { it.title })
    }

    @Test
    fun includedRecipesFollowWithTheirOwnIngredientsAndPreparation() {
        val soup = Recipe(2, "Soup")
        val croutons = Recipe(3, "Croutons", description = "<p>Roast</p>")
        val sections = buildRecipeSections(
            recipe = soup,
            recipes = listOf(soup, croutons),
            resolved = listOf(row(1, recipeId = 2), row(5, recipeId = 2, childRecipeId = 3)),
            format = format,
            groupByProductGroup = false,
            checksStock = true,
        )

        assertEquals(listOf("Soup", "Croutons"), sections.map { it.name })
        assertEquals(listOf(5), sections[1].groups.single().lines.map { it.ingredientId })
        assertEquals("<p>Roast</p>", sections[1].description)
    }

    @Test
    fun missingProductsAreEachProductOnceByName() {
        val missing = missingProducts(
            resolved = listOf(row(1, missing = 1.0), row(2, missing = 2.0), row(3)),
            products = listOf(milk),
        )

        assertEquals(listOf(MissingProduct(1, "Milk")), missing)
    }
}
