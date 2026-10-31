package com.patrickzedler.grocy.core.model.recipes

/**
 * One of Grocy's normal recipes (meal plan entries are recipes too, the app leaves them out).
 * The ingredients of [baseServings] servings are scaled to [desiredServings].
 */
data class Recipe(
    val id: Int,
    val name: String,
    /** Grocy's "Preparation", HTML. */
    val description: String? = null,
    val pictureFileName: String? = null,
    val baseServings: Double = 1.0,
    val desiredServings: Double = 1.0,
    /** Missing ingredients go on the shopping list without checking what is on it already. */
    val notCheckShoppingList: Boolean = false,
    /** Grocy's "Produces product": consuming the recipe adds it to the stock. */
    val productId: Int? = null,
    /** Userfield values by name. */
    val userfields: Map<String, String?> = emptyMap(),
)

/**
 * What Grocy computed for a recipe at its desired servings (`/recipes/fulfillment`, Grocy's view
 * `recipes_resolved`): whether the stock covers it, what it costs and how due its ingredients are.
 */
data class RecipeFulfillment(
    val recipeId: Int,
    val needFulfilled: Boolean,
    val needFulfilledWithShoppingList: Boolean,
    val missingProductsCount: Int,
    val costs: Double,
    val costsPerServing: Double,
    /** Energy per serving. */
    val calories: Double,
    /** The higher, the more ingredients in stock are due soon, overdue or expired. */
    val dueScore: Int,
    /** The ingredients' products, which Grocy's search finds the recipe by. */
    val productNames: String,
    /** At least one ingredient has no price. */
    val pricesIncomplete: Boolean,
) {
    val status: RecipeStatus
        get() = when {
            needFulfilled -> RecipeStatus.ENOUGH_IN_STOCK
            needFulfilledWithShoppingList -> RecipeStatus.ON_SHOPPING_LIST
            else -> RecipeStatus.NOT_ENOUGH_IN_STOCK
        }
}

/** Grocy's "Requirements fulfilled" of a recipe or an ingredient, in its order. */
enum class RecipeStatus { ENOUGH_IN_STOCK, ON_SHOPPING_LIST, NOT_ENOUGH_IN_STOCK }

/** An ingredient of a recipe as its form edits it (`recipes_pos`). */
data class RecipeIngredient(
    val id: Int,
    val recipeId: Int,
    val productId: Int,
    /** In unit [quId]; Grocy's "Only check if any amount is in stock" allows any unit. */
    val amount: Double,
    val quId: Int?,
    val note: String? = null,
    val onlyCheckSingleUnitInStock: Boolean = false,
    /** The headline the ingredient is grouped under. */
    val ingredientGroup: String? = null,
    val notCheckStockFulfillment: Boolean = false,
    /** Shown instead of the amount, e.g. "a pinch". */
    val variableAmount: String? = null,
    val priceFactor: Double = 1.0,
    /** Null where the server is older than Grocy 4.4, which added it. */
    val roundUp: Boolean? = null,
)

/** A recipe included in another one with [servings] of its own servings (`recipes_nestings`). */
data class RecipeNesting(
    val id: Int,
    val recipeId: Int,
    val includesRecipeId: Int,
    val servings: Double,
)

/**
 * An ingredient as Grocy resolved it for recipe [recipeId] at its desired servings (the view
 * `recipes_pos_resolved`), included recipes' ingredients too ([isNested]). Amounts are in
 * unit [quId] of the ingredient, the stock amount in the product's stock unit.
 */
data class ResolvedRecipeIngredient(
    val recipeId: Int,
    val ingredientId: Int,
    val productId: Int,
    val recipeAmount: Double,
    val stockAmount: Double,
    val needFulfilled: Boolean,
    val missingAmount: Double,
    val amountOnShoppingList: Double,
    val needFulfilledWithShoppingList: Boolean,
    val quId: Int?,
    val costs: Double,
    val isNested: Boolean,
    val ingredientGroup: String?,
    val productGroup: String?,
    /** The recipe the ingredient belongs to, [recipeId] itself unless [isNested]. */
    val childRecipeId: Int,
    val note: String?,
    val variableAmount: String?,
    val onlyCheckSingleUnitInStock: Boolean,
    val calories: Double,
    val productActive: Boolean,
    /** Grocy's due score of the product: 0 fine, 1 due soon, 10 overdue, 20 expired. */
    val dueScore: Int,
    /** The sub product used while the parent product is not in stock. */
    val productIdEffective: Int,
) {
    val status: RecipeStatus
        get() = when {
            needFulfilled -> RecipeStatus.ENOUGH_IN_STOCK
            needFulfilledWithShoppingList -> RecipeStatus.ON_SHOPPING_LIST
            else -> RecipeStatus.NOT_ENOUGH_IN_STOCK
        }
}

/** A section of Grocy's meal plan, e.g. breakfast; the default one has no name. */
data class MealPlanSection(val id: Int, val name: String, val sortNumber: Int?)

/** Grocy's grocycode of recipe [recipeId], "grcy:r:<id>". */
fun recipeGrocycode(recipeId: Int): String = "grcy:r:$recipeId"
