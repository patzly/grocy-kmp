package com.patrickzedler.grocy.feature.recipes.impl.recipe

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.text.formatDecimal

/** An ingredient as Grocy's recipe card lists it, its texts formatted. */
internal data class IngredientLine(
    val ingredientId: Int,
    val productId: Int,
    /** "2 Pieces Milk", or the variable amount instead of amount and unit. */
    val text: String,
    val productName: String,
    /** The sub product used while the parent [productName] is not in stock, its id and name. */
    val substitute: Pair<Int, String>?,
    /** Null where Grocy does not check the stock (without the stock feature). */
    val status: RecipeStatus?,
    /** The stock, "3 Pieces", in the product's stock unit. */
    val stockAmount: String,
    /** What is missing and what is on the shopping list, in the product's stock unit. */
    val missingAmount: String,
    val amountOnShoppingList: String,
    val isVariableAmount: Boolean,
    val note: String?,
    val productActive: Boolean,
    /** Like Grocy's highlighted names: due soon, overdue or expired products in stock. */
    val dueStatus: DueStatus?,
    val costs: Double,
    val calories: Double,
)

/** Ingredients under one of Grocy's headlines; [title] null for the ones without group. */
internal data class IngredientGroup(val title: String?, val lines: List<IngredientLine>)

/** A recipe of the card: the recipe itself or one it includes, with its own ingredients. */
internal data class RecipeSection(
    val recipeId: Int,
    val name: String,
    /** Grocy's "Preparation" as HTML, null without. */
    val description: String?,
    val groups: List<IngredientGroup>,
)

/** A product the recipe misses, a choice of Grocy's "Put missing products on shopping list". */
internal data class MissingProduct(val productId: Int, val name: String)

/** What the card formats with: the user's decimal places and the app language's units. */
internal class IngredientFormat(
    val products: List<Product>,
    val units: List<QuantityUnit>,
    val conversions: List<ResolvedQuantityUnitConversion>,
    val decimals: Int,
    val decimalSeparator: Char,
    val language: String,
)

/**
 * Grocy's recipe card of [recipe] at its desired servings: its own ingredients first, then each
 * recipe it includes ([recipes] names them) with its ingredients as resolved for this recipe
 * and its preparation, like Grocy's tabs of included recipes. Ingredients are grouped by their
 * group and, with [groupByProductGroup], their product group, in the order Grocy's view sorts.
 */
internal fun buildRecipeSections(
    recipe: Recipe,
    recipes: List<Recipe>,
    resolved: List<ResolvedRecipeIngredient>,
    format: IngredientFormat,
    groupByProductGroup: Boolean,
    checksStock: Boolean,
): List<RecipeSection> {
    val recipesById = recipes.associateBy { it.id }
    val own = resolved.filter { !it.isNested || it.childRecipeId == recipe.id }
    val included = resolved.filter { it.isNested && it.childRecipeId != recipe.id }
        .groupBy { it.childRecipeId }
        // Grocy orders them by name
        .toList()
        .sortedBy { (id, _) -> recipesById[id]?.name?.lowercase() }
    fun section(id: Int, rows: List<ResolvedRecipeIngredient>) = RecipeSection(
        recipeId = id,
        name = recipesById[id]?.name.orEmpty(),
        description = recipesById[id]?.description,
        groups = groupsOf(rows, format, groupByProductGroup, checksStock),
    )
    return listOf(section(recipe.id, own)) + included.map { (id, rows) -> section(id, rows) }
}

private fun groupsOf(
    rows: List<ResolvedRecipeIngredient>,
    format: IngredientFormat,
    groupByProductGroup: Boolean,
    checksStock: Boolean,
): List<IngredientGroup> = rows
    .sortedWith(
        compareBy<ResolvedRecipeIngredient, String?>(nullsFirst()) { it.ingredientGroup }
            .thenBy(nullsFirst()) { it.productGroup.takeIf { groupByProductGroup } },
    )
    .groupBy { row ->
        listOfNotNull(row.ingredientGroup, row.productGroup.takeIf { groupByProductGroup })
            .joinToString(" · ")
            .ifEmpty { null }
    }
    .map { (title, lines) ->
        IngredientGroup(title, lines.map { ingredientLine(it, format, checksStock) })
    }

/**
 * An ingredient's line: the amount in its unit (Grocy keeps it in the stock unit, the unit only
 * shows it, unless only "any amount in stock" is checked), the product and the stock status.
 */
internal fun ingredientLine(
    row: ResolvedRecipeIngredient,
    format: IngredientFormat,
    checksStock: Boolean,
): IngredientLine {
    val products = format.products.associateBy { it.id }
    val units = format.units.associateBy { it.id }
    val product = products[row.productId]
    val stockUnit = product?.quIdStock?.let { units[it] }
    val factor = if (row.onlyCheckSingleUnitInStock || row.quId == product?.quIdStock) {
        1.0
    } else {
        format.conversions.firstOrNull {
            it.productId == row.productId && it.fromQuId == product?.quIdStock &&
                it.toQuId == row.quId
        }?.factor ?: 1.0
    }
    val amount = row.recipeAmount * factor
    val name = product?.name.orEmpty()
    fun amountText(value: Double, unit: QuantityUnit?) = listOfNotNull(
        formatDecimal(value, format.decimals, format.decimalSeparator),
        unit?.nameFor(value, format.language),
    ).joinToString(" ")
    val quantity = row.variableAmount ?: amountText(amount, row.quId?.let { units[it] })
    return IngredientLine(
        ingredientId = row.ingredientId,
        productId = row.productId,
        text = "$quantity $name".trim(),
        productName = name,
        substitute = row.productIdEffective.takeIf { it != row.productId }?.let { id ->
            id to products[id]?.name.orEmpty()
        },
        status = row.status.takeIf { checksStock },
        stockAmount = amountText(row.stockAmount, stockUnit),
        missingAmount = amountText(row.missingAmount, stockUnit),
        amountOnShoppingList = amountText(row.amountOnShoppingList, stockUnit),
        isVariableAmount = row.variableAmount != null,
        note = row.note,
        productActive = row.productActive,
        dueStatus = when (row.dueScore) {
            DUE_SCORE_EXPIRED -> DueStatus.EXPIRED
            DUE_SCORE_OVERDUE -> DueStatus.OVERDUE
            DUE_SCORE_DUE_SOON -> DueStatus.DUE_SOON
            else -> null
        },
        costs = row.costs,
        calories = row.calories,
    )
}

/**
 * The products Grocy's "Put missing products on shopping list" offers: the recipe's and its
 * included recipes' ingredients with a missing amount, each product once, by name.
 */
internal fun missingProducts(
    resolved: List<ResolvedRecipeIngredient>,
    products: List<Product>,
): List<MissingProduct> {
    val names = products.associate { it.id to it.name }
    return resolved.filter { it.missingAmount > 0 }
        .map { it.productId }
        .distinct()
        .map { MissingProduct(it, names[it].orEmpty()) }
        .sortedBy { it.name.lowercase() }
}

// Grocy's due scores of a product in recipes_pos_resolved
private const val DUE_SCORE_DUE_SOON = 1
private const val DUE_SCORE_OVERDUE = 10
private const val DUE_SCORE_EXPIRED = 20
