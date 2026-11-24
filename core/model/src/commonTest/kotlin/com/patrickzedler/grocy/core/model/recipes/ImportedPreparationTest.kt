package com.patrickzedler.grocy.core.model.recipes

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportedPreparationTest {

    private val texts = ImportTexts(
        ingredients = "Ingredients",
        preparation = "Preparation",
        prepTime = "Preparation time",
        cookTime = "Cooking time",
        totalTime = "Total time",
        nutrition = "Per serving",
        nutrients = mapOf(RecipeNutrient.FAT to "Fat", RecipeNutrient.PROTEIN to "Protein"),
        category = "Category",
        cuisine = "Cuisine",
        author = "Author",
        source = "Source",
        minutes = { "$it min" },
    )

    @Test
    fun everythingTheSiteTellsIsKeptInTheOrderOfACookbookPage() {
        val recipe = ImportedRecipe(
            name = "Carbonara",
            sourceUrl = "https://example.com/carbonara",
            description = "Roman classic.",
            ingredients = listOf("500 g Spaghetti", "6 Eigelbe"),
            instructions = listOf(InstructionSection(null, listOf("Boil.", "Mix."))),
            prepMinutes = 10,
            totalMinutes = 25,
            nutrition = mapOf(
                RecipeNutrient.PROTEIN to "20 g",
                RecipeNutrient.CALORIES to "520 kcal",
                RecipeNutrient.FAT to "25 g",
            ),
            category = "Main",
            author = "Anna",
        )

        assertEquals(
            """
            Roman classic.

            Ingredients
            • 500 g Spaghetti
            • 6 Eigelbe

            Preparation
            1. Boil.
            2. Mix.

            Preparation time: 10 min · Total time: 25 min

            Per serving: 520 kcal · Fat 25 g · Protein 20 g

            Category: Main · Author: Anna

            Source: https://example.com/carbonara
            """.trimIndent(),
            importedPreparation(recipe, texts),
        )
    }

    @Test
    fun sectionsKeepTheirHeadlines() {
        val recipe = ImportedRecipe(
            name = "Cake",
            sourceUrl = "https://example.com/cake",
            instructions = listOf(
                InstructionSection("Dough", listOf("Knead.")),
                InstructionSection("Topping", listOf("Crumble.", "Bake.")),
            ),
        )

        assertEquals(
            "Dough\nKnead.\n\nTopping\nCrumble.\nBake.\n\nSource: https://example.com/cake",
            importedPreparation(recipe, texts),
        )
    }

    @Test
    fun aRecipeWithoutTextsStillNamesItsSource() {
        val recipe = ImportedRecipe(name = "Toast", sourceUrl = "https://example.com/toast")

        assertEquals("Source: https://example.com/toast", importedPreparation(recipe, texts))
    }
}
