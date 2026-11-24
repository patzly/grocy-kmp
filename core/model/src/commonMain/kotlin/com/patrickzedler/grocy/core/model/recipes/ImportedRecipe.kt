package com.patrickzedler.grocy.core.model.recipes

/**
 * A recipe as a recipe site describes it (schema.org `Recipe`, which nearly every site embeds
 * for search engines): texts as the site wrote them, nothing matched to Grocy yet.
 */
data class ImportedRecipe(
    val name: String,
    /** The page it came from, kept in the preparation. */
    val sourceUrl: String,
    val description: String? = null,
    /** The first number of the site's yield, "4 Portionen" is 4. */
    val servings: Int? = null,
    /** One line per ingredient, e.g. "200 g Mehl (Type 405)". */
    val ingredients: List<String> = emptyList(),
    val instructions: List<InstructionSection> = emptyList(),
    val imageUrl: String? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val totalMinutes: Int? = null,
    /** Per serving, as the site writes the values, e.g. "520 kcal" or "25 g". */
    val nutrition: Map<RecipeNutrient, String> = emptyMap(),
    val category: String? = null,
    val cuisine: String? = null,
    val author: String? = null,
)

/** Steps of the preparation, under a headline of the site's where it has several parts. */
data class InstructionSection(val name: String?, val steps: List<String>)

/** The nutrition facts of schema.org, in the order sites and packages list them. */
enum class RecipeNutrient {
    CALORIES,
    FAT,
    SATURATED_FAT,
    CARBOHYDRATES,
    SUGARS,
    FIBER,
    PROTEIN,
    SODIUM,
}

/**
 * The words of the preparation text, from the app's translations.
 *
 * @param minutes formats a number of minutes, e.g. "1 h 15 min".
 */
class ImportTexts(
    val ingredients: String,
    val preparation: String,
    val prepTime: String,
    val cookTime: String,
    val totalTime: String,
    val nutrition: String,
    val nutrients: Map<RecipeNutrient, String>,
    val category: String,
    val cuisine: String,
    val author: String,
    val source: String,
    val minutes: (Int) -> String,
)

/**
 * Grocy keeps a recipe's text in its preparation only, so everything else the site tells goes
 * there too, in the order of a cookbook page: the description, the ingredients as the site wrote
 * them (also those that become Grocy ingredients, their original wording stays readable), the
 * steps, times, nutrition per serving, category, cuisine, author and the source.
 */
fun importedPreparation(recipe: ImportedRecipe, texts: ImportTexts): String {
    val parts = mutableListOf<String>()
    recipe.description?.let(parts::add)
    if (recipe.ingredients.isNotEmpty()) {
        parts += "${texts.ingredients}\n" + recipe.ingredients.joinToString("\n") { "• $it" }
    }
    val numbered = recipe.instructions.size == 1 || recipe.instructions.all { it.name == null }
    var step = 0
    recipe.instructions.forEachIndexed { index, section ->
        val headline = section.name ?: texts.preparation.takeIf { index == 0 }
        val steps = section.steps.joinToString("\n") { text ->
            step++
            if (numbered && section.steps.size > 1) "$step. $text" else text
        }
        parts += listOfNotNull(headline, steps).joinToString("\n")
    }
    val times = listOfNotNull(
        recipe.prepMinutes?.let { "${texts.prepTime}: ${texts.minutes(it)}" },
        recipe.cookMinutes?.let { "${texts.cookTime}: ${texts.minutes(it)}" },
        recipe.totalMinutes?.let { "${texts.totalTime}: ${texts.minutes(it)}" },
    )
    if (times.isNotEmpty()) parts += times.joinToString(SEPARATOR)
    if (recipe.nutrition.isNotEmpty()) {
        val values = RecipeNutrient.entries.mapNotNull { nutrient ->
            val value = recipe.nutrition[nutrient] ?: return@mapNotNull null
            // Calories need no name, their unit tells
            if (nutrient == RecipeNutrient.CALORIES) {
                value
            } else {
                "${texts.nutrients[nutrient]} $value"
            }
        }
        parts += "${texts.nutrition}: ${values.joinToString(SEPARATOR)}"
    }
    val about = listOfNotNull(
        recipe.category?.let { "${texts.category}: $it" },
        recipe.cuisine?.let { "${texts.cuisine}: $it" },
        recipe.author?.let { "${texts.author}: $it" },
    )
    if (about.isNotEmpty()) parts += about.joinToString(SEPARATOR)
    parts += "${texts.source}: ${recipe.sourceUrl}"
    return parts.joinToString("\n\n")
}

private const val SEPARATOR = " · "
