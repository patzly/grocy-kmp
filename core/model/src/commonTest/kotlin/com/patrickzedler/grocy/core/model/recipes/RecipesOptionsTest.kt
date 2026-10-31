package com.patrickzedler.grocy.core.model.recipes

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipesOptionsTest {

    @Test
    fun nothingStoredMeansGrocysDefaults() {
        val options = RecipesOptions.of(null, null, null, null, null)

        assertEquals(
            setOf(RecipeField.DUE_SCORE, RecipeField.REQUIREMENTS_FULFILLED),
            options.fields,
        )
        assertEquals(RecipeGrouping.NONE, options.grouping)
        assertEquals(RecipeField.NAME, options.sortField)
        assertEquals(true, options.ascending)
        assertEquals(RecipesView.LIST, options.view)
    }

    @Test
    fun storedOptionsAreReadBack() {
        val options = RecipesOptions(
            fields = setOf(RecipeField.REQUIREMENTS_FULFILLED, RecipeField.DESIRED_SERVINGS),
            grouping = RecipeGrouping.REQUIREMENTS_FULFILLED,
            sortField = RecipeField.DUE_SCORE,
            ascending = false,
            view = RecipesView.GALLERY,
        )

        val stored = RecipesOptions.storedFields(options.fields)

        assertEquals("DESIRED_SERVINGS,REQUIREMENTS_FULFILLED", stored)
        assertEquals(
            options,
            RecipesOptions.of(stored, "REQUIREMENTS_FULFILLED", "DUE_SCORE", "false", "GALLERY"),
        )
    }

    @Test
    fun unknownValuesOfANewerVersionAreLeftOut() {
        val options = RecipesOptions.of("DUE_SCORE,CALORIES", "COSTS", "COSTS", "maybe", "TABLE")

        assertEquals(RecipesOptions(fields = setOf(RecipeField.DUE_SCORE)), options)
    }
}
