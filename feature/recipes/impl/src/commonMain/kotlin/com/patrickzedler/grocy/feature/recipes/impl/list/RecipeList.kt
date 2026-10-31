package com.patrickzedler.grocy.feature.recipes.impl.list

import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeGrouping
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.FuzzySearch

/** A row of Grocy's recipes table, a card of its gallery. */
internal data class RecipeListItem(
    val id: Int,
    val name: String,
    val desiredServings: Double,
    /** Null until the server computed it. */
    val dueScore: Int?,
    /** Null until the server computed it. */
    val status: RecipeStatus?,
    val missingProductsCount: Int,
    val pictureFileName: String?,
)

/** What a group of the list is titled by, the value of the column it is grouped by. */
internal sealed interface RecipeGroupTitle {
    data class Servings(val servings: Double) : RecipeGroupTitle

    data class DueScore(val score: Int) : RecipeGroupTitle

    data class Status(val status: RecipeStatus) : RecipeGroupTitle

    /** Rows without a value, last like Grocy's empty group. */
    data object None : RecipeGroupTitle
}

/** The recipes of one group; [title] is null while the list is not grouped. */
internal data class RecipeGroup(val title: RecipeGroupTitle?, val items: List<RecipeListItem>)

internal data class RecipeSort(val field: RecipeField, val ascending: Boolean)

/** Grocy's status filter of the recipes; null shows all. */
internal data class RecipesFilter(val status: RecipeStatus? = null) {
    val isActive: Boolean
        get() = status != null
}

/** The list as built for the screen. */
internal data class RecipeListResult(
    val groups: List<RecipeGroup>,
    /** Recipes per status before the status filter, for its menu. */
    val statusCounts: Map<RecipeStatus, Int>,
)

/** The fields besides the name the server's features offer; Grocy's status needs the stock. */
internal fun availableFields(config: ServerConfig): List<RecipeField> = listOfNotNull(
    RecipeField.DESIRED_SERVINGS,
    RecipeField.DUE_SCORE,
    RecipeField.REQUIREMENTS_FULFILLED.takeIf { config.stock },
)

/** Grocy's groupable columns the server's features offer, "None" first. */
internal fun availableGroupings(config: ServerConfig): List<RecipeGrouping> = listOfNotNull(
    RecipeGrouping.NONE,
    RecipeGrouping.DESIRED_SERVINGS,
    RecipeGrouping.DUE_SCORE,
    RecipeGrouping.REQUIREMENTS_FULFILLED.takeIf { config.stock },
)

/** Grocy's default order, by name. */
internal val DefaultSort = RecipeSort(RecipeField.NAME, ascending = true)

/** The chosen order while its field is shown, otherwise Grocy's default. */
internal fun effectiveSort(field: RecipeField, ascending: Boolean, shown: List<RecipeField>) =
    if (field == RecipeField.NAME || field in shown) RecipeSort(field, ascending) else DefaultSort

/**
 * Grocy's recipes page: [recipes] joined with what the server computed for them, filtered by
 * status and [query] (fuzzy, the name or an ingredient's product like Grocy's search), sorted
 * like a click on a column header (ties by name) and grouped by a column.
 */
internal fun buildRecipeList(
    recipes: List<Recipe>,
    fulfillment: List<RecipeFulfillment>,
    filter: RecipesFilter,
    sort: RecipeSort,
    grouping: RecipeGrouping,
    query: String,
): RecipeListResult {
    val byRecipe = fulfillment.associateBy { it.recipeId }
    val searched = recipes.filter { recipe ->
        query.isBlank() || searchTexts(recipe, byRecipe[recipe.id]).any {
            FuzzySearch.score(query, it) != null
        }
    }
    val items = searched.map { recipe ->
        val computed = byRecipe[recipe.id]
        RecipeListItem(
            id = recipe.id,
            name = recipe.name,
            desiredServings = recipe.desiredServings,
            dueScore = computed?.dueScore,
            status = computed?.status,
            missingProductsCount = computed?.missingProductsCount ?: 0,
            pictureFileName = recipe.pictureFileName,
        )
    }
    val statusCounts = items.mapNotNull { it.status }.groupingBy { it }.eachCount()
    val filtered = items.filter { filter.status == null || it.status == filter.status }
    val sorted = filtered.sortedWith(comparatorOf(sort))
    val groups = if (grouping == RecipeGrouping.NONE) {
        listOf(RecipeGroup(title = null, items = sorted)).filter { it.items.isNotEmpty() }
    } else {
        sorted.groupBy { groupTitleOf(it, grouping) }
            .map { (title, items) -> RecipeGroup(title, items) }
            .sortedWith(compareBy(nullsLast()) { it.title?.let(::groupOrder) })
    }
    return RecipeListResult(groups, statusCounts)
}

/** What the search finds a recipe by: its name and its ingredients' products. */
private fun searchTexts(recipe: Recipe, fulfillment: RecipeFulfillment?): List<String> =
    listOf(recipe.name) + fulfillment?.productNames?.split(',').orEmpty()

private fun comparatorOf(sort: RecipeSort): Comparator<RecipeListItem> {
    val byName = compareBy<RecipeListItem> { it.name.lowercase() }
    val byField: Comparator<RecipeListItem> = when (sort.field) {
        RecipeField.NAME -> byName
        RecipeField.DESIRED_SERVINGS -> compareBy { it.desiredServings }
        // Not computed yet sorts last either way
        RecipeField.DUE_SCORE -> compareBy(nullsLast()) { it.dueScore }
        // Grocy sorts the column by the missing ingredients
        RecipeField.REQUIREMENTS_FULFILLED ->
            compareBy<RecipeListItem, Int?>(nullsLast()) { it.status?.ordinal }
                .thenBy { it.missingProductsCount }
    }
    val ordered = if (sort.ascending) byField else byField.reversed()
    return ordered.then(byName)
}

private fun groupTitleOf(item: RecipeListItem, grouping: RecipeGrouping): RecipeGroupTitle =
    when (grouping) {
        RecipeGrouping.NONE -> RecipeGroupTitle.None
        RecipeGrouping.DESIRED_SERVINGS -> RecipeGroupTitle.Servings(item.desiredServings)
        RecipeGrouping.DUE_SCORE ->
            item.dueScore?.let { RecipeGroupTitle.DueScore(it) } ?: RecipeGroupTitle.None
        RecipeGrouping.REQUIREMENTS_FULFILLED ->
            item.status?.let { RecipeGroupTitle.Status(it) } ?: RecipeGroupTitle.None
    }

/** Groups in the order of their values, "None" last. */
private fun groupOrder(title: RecipeGroupTitle): Double? = when (title) {
    is RecipeGroupTitle.Servings -> title.servings
    is RecipeGroupTitle.DueScore -> title.score.toDouble()
    is RecipeGroupTitle.Status -> title.status.ordinal.toDouble()
    RecipeGroupTitle.None -> null
}
