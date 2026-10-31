package com.patrickzedler.grocy.feature.recipes.impl.form

import com.patrickzedler.grocy.core.forms.AmountUnit
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.feature.recipes.impl.actions.MealPlanField
import com.patrickzedler.grocy.feature.recipes.impl.actions.validateMealPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeFormTest {

    private val piece = AmountUnit(QuantityUnit(1, "Piece"), 1.0)

    // A pack holds six pieces: a sixth of a pack per piece in stock
    private val pack = AmountUnit(QuantityUnit(2, "Pack"), 1.0 / 6)

    @Test
    fun recipeNeedsANameAndServingsAboveZero() {
        val errors = RecipeFormValues(name = " ", baseServings = "0").validate(decimals = 2)

        assertEquals(FieldError.Required, errors[ObjectField.NAME])
        assertTrue(errors[ObjectField.AMOUNT] is FieldError.InvalidNumber)
        assertTrue(RecipeFormValues(name = "Soup").validate(decimals = 2).isEmpty())
    }

    @Test
    fun unchangedPreparationKeepsItsHtml() {
        val original = Recipe(3, "Soup", description = "<p><b>Boil</b> water</p>")
        val values = original.toFormValues(decimals = 2, decimalSeparator = '.')

        assertEquals("<p><b>Boil</b> water</p>", values.toRecipe(3, original).description)
        assertEquals(
            "<p>Boil water &amp; salt</p>",
            values.copy(preparation = "Boil water & salt").toRecipe(3, original).description,
        )
    }

    @Test
    fun savedRecipeKeepsWhatTheFormDoesNotEdit() {
        val original = Recipe(
            id = 3,
            name = "Soup",
            pictureFileName = "soup.jpg",
            desiredServings = 6.0,
            userfields = mapOf("vegan" to "1"),
        )

        val saved = RecipeFormValues(name = " Hot soup ", baseServings = "2.5")
            .toRecipe(3, original)

        assertEquals("Hot soup", saved.name)
        assertEquals(2.5, saved.baseServings)
        assertEquals(6.0, saved.desiredServings)
        assertEquals("soup.jpg", saved.pictureFileName)
        assertEquals(mapOf("vegan" to "1"), saved.userfields)
    }

    @Test
    fun newRecipeHasGrocysSingleDesiredServing() {
        val recipe = RecipeFormValues(name = "Soup", baseServings = "4").toRecipe(0, null)

        assertEquals(1.0, recipe.desiredServings)
    }

    @Test
    fun ingredientAmountIsSavedInTheStockUnit() {
        val ingredient = IngredientFormValues(productId = 1, amount = "2", quId = 2)
            .toIngredient(0, recipeId = 3, units = listOf(piece, pack), null, false, decimals = 2)

        // Two packs are twelve pieces
        assertEquals(12.0, ingredient.amount, 1e-9)
        assertEquals(2, ingredient.quId)
    }

    @Test
    fun convertedAmountIsRoundedLikeGrocysPicker() {
        // Grocy's conversions are rounded to ten places, 1/6 here
        val sixth = AmountUnit(QuantityUnit(2, "Pack"), 0.1666666667)

        val ingredient = IngredientFormValues(productId = 1, amount = "1", quId = 2)
            .toIngredient(0, recipeId = 3, units = listOf(sixth), null, false, decimals = 0)

        // At least one decimal place, like Grocy
        assertEquals(6.0, ingredient.amount)
    }

    @Test
    fun anyAmountInStockSavesTheAmountAsTyped() {
        val ingredient = IngredientFormValues(
            productId = 1,
            amount = "2",
            quId = 2,
            onlyCheckSingleUnitInStock = true,
        ).toIngredient(0, recipeId = 3, units = listOf(piece, pack), null, false, decimals = 2)

        assertEquals(2.0, ingredient.amount)
    }

    @Test
    fun savedIngredientShowsInItsUnitAgain() {
        val saved = RecipeIngredient(
            id = 5,
            recipeId = 3,
            productId = 1,
            amount = 12.0,
            quId = 2,
            priceFactor = 2.0,
        )

        val values = saved.toFormValues(listOf(piece, pack), decimals = 2, decimalSeparator = ',')

        assertEquals("2", values.amount)
        assertEquals("2", values.priceFactor)
    }

    @Test
    fun roundUpIsOnlySentWhereTheServerKnowsIt() {
        val values = IngredientFormValues(productId = 1, amount = "1", quId = 1)
        val units = listOf(piece)

        assertNull(values.toIngredient(0, 3, units, null, false, decimals = 2).roundUp)
        assertEquals(
            false,
            values.toIngredient(0, 3, units, null, supportsRoundUp = true, decimals = 2).roundUp,
        )
        // Switched on, the user asks for it
        assertEquals(
            true,
            values.copy(roundUp = true).toIngredient(0, 3, units, null, false, 2).roundUp,
        )
    }

    @Test
    fun ingredientTextsAreTrimmedAndEmptyOnesLeftOut() {
        val ingredient = IngredientFormValues(
            productId = 1,
            amount = "1",
            quId = 1,
            group = " Dough ",
            note = " ",
            variableAmount = "",
        ).toIngredient(0, 3, listOf(piece), null, false, decimals = 2)

        assertEquals("Dough", ingredient.ingredientGroup)
        assertNull(ingredient.note)
        assertNull(ingredient.variableAmount)
    }

    @Test
    fun ingredientNeedsProductUnitAndPriceFactor() {
        val errors = IngredientFormValues(priceFactor = "0").validate(decimals = 2)

        assertEquals(FieldError.Required, errors[IngredientField.PRODUCT])
        assertEquals(FieldError.Required, errors[IngredientField.AMOUNT])
        assertEquals(FieldError.Required, errors[IngredientField.UNIT])
        assertTrue(errors[IngredientField.PRICE_FACTOR] is FieldError.InvalidNumber)
    }

    @Test
    fun zeroAmountIsOnlyFineWhenTheStockIsChecked() {
        val values = IngredientFormValues(productId = 1, amount = "0", quId = 1)

        assertTrue(values.validate(decimals = 2).isEmpty())
        assertTrue(
            values.copy(onlyCheckSingleUnitInStock = true).validate(decimals = 2)
                .containsKey(IngredientField.AMOUNT),
        )
    }

    @Test
    fun recipeCannotIncludeItself() {
        val itself = NestingFormValues(recipeId = 3).validate(decimals = 2, ownRecipeId = 3)

        assertEquals(FieldError.Required, itself[ObjectField.NAME])
        assertEquals(
            RecipeNesting(0, recipeId = 3, includesRecipeId = 4, servings = 2.0),
            NestingFormValues(recipeId = 4, servings = "2").toNesting(0, ownRecipeId = 3),
        )
    }

    @Test
    fun mealPlanNeedsADayAndServings() {
        val errors = validateMealPlan(day = "2026-02-30", servings = "", decimals = 2)

        assertEquals(FieldError.InvalidDate, errors[MealPlanField.DAY])
        assertEquals(FieldError.Required, errors[MealPlanField.SERVINGS])
        assertTrue(validateMealPlan("2026-11-02", "2", decimals = 2).isEmpty())
    }
}
