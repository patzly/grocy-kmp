package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A recipe of Grocy's `recipes`, all of its fields. */
@Serializable
internal data class RecipeObjectDto(
    val id: Int,
    val name: FlexString = null,
    val description: FlexString = null,
    @SerialName("picture_file_name") val pictureFileName: FlexString = null,
    @SerialName("base_servings") val baseServings: FlexDouble = null,
    @SerialName("desired_servings") val desiredServings: FlexDouble = null,
    @SerialName("not_check_shoppinglist") val notCheckShoppingList: FlexBoolean = null,
    @SerialName("product_id") val productId: FlexInt = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Recipe(
        id = id,
        name = name.orEmpty(),
        description = description?.takeIf { it.isNotBlank() },
        pictureFileName = pictureFileName?.takeIf { it.isNotBlank() },
        // Grocy's column defaults
        baseServings = baseServings ?: 1.0,
        desiredServings = desiredServings ?: 1.0,
        notCheckShoppingList = notCheckShoppingList ?: false,
        productId = productId,
        userfields = userfields.orEmpty(),
    )
}

/** The fields of Grocy's recipe form; new recipes are normal ones like Grocy creates them. */
internal fun Recipe.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("base_servings", baseServings)
    put("desired_servings", desiredServings)
    put("not_check_shoppinglist", flag(notCheckShoppingList))
    // Null clears them, like Grocy's form does
    put("product_id", productId)
    put("picture_file_name", pictureFileName)
    put("type", "normal")
}

/** A row of Grocy's `recipes_resolved` as `/recipes/fulfillment` returns it. */
@Serializable
internal data class RecipeFulfillmentDto(
    @SerialName("recipe_id") val recipeId: Int,
    @SerialName("need_fulfilled") val needFulfilled: FlexBoolean = null,
    @SerialName("need_fulfilled_with_shopping_list")
    val needFulfilledWithShoppingList: FlexBoolean = null,
    @SerialName("missing_products_count") val missingProductsCount: FlexInt = null,
    val costs: FlexDouble = null,
    @SerialName("costs_per_serving") val costsPerServing: FlexDouble = null,
    val calories: FlexDouble = null,
    @SerialName("due_score") val dueScore: FlexInt = null,
    @SerialName("product_names_comma_separated") val productNames: FlexString = null,
    @SerialName("prices_incomplete") val pricesIncomplete: FlexBoolean = null,
) {
    fun toModel() = RecipeFulfillment(
        recipeId = recipeId,
        // A recipe without ingredients is fulfilled in Grocy's view
        needFulfilled = needFulfilled ?: true,
        needFulfilledWithShoppingList = needFulfilledWithShoppingList ?: true,
        missingProductsCount = missingProductsCount ?: 0,
        costs = costs ?: 0.0,
        costsPerServing = costsPerServing ?: 0.0,
        calories = calories ?: 0.0,
        dueScore = dueScore ?: 0,
        productNames = productNames.orEmpty(),
        pricesIncomplete = pricesIncomplete ?: false,
    )
}

/** An ingredient of Grocy's `recipes_pos`. */
@Serializable
internal data class RecipeIngredientDto(
    val id: Int,
    @SerialName("recipe_id") val recipeId: FlexInt = null,
    @SerialName("product_id") val productId: FlexInt = null,
    val amount: FlexDouble = null,
    @SerialName("qu_id") val quId: FlexInt = null,
    val note: FlexString = null,
    @SerialName("only_check_single_unit_in_stock")
    val onlyCheckSingleUnitInStock: FlexBoolean = null,
    @SerialName("ingredient_group") val ingredientGroup: FlexString = null,
    @SerialName("not_check_stock_fulfillment") val notCheckStockFulfillment: FlexBoolean = null,
    @SerialName("variable_amount") val variableAmount: FlexString = null,
    @SerialName("price_factor") val priceFactor: FlexDouble = null,
    @SerialName("round_up") val roundUp: FlexBoolean = null,
) {
    fun toModel() = RecipeIngredient(
        id = id,
        recipeId = recipeId ?: 0,
        productId = productId ?: 0,
        amount = amount ?: 0.0,
        quId = quId,
        note = note?.takeIf { it.isNotBlank() },
        onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock ?: false,
        ingredientGroup = ingredientGroup?.takeIf { it.isNotBlank() },
        notCheckStockFulfillment = notCheckStockFulfillment ?: false,
        variableAmount = variableAmount?.takeIf { it.isNotBlank() },
        priceFactor = priceFactor ?: 1.0,
        roundUp = roundUp,
    )
}

/**
 * The fields of Grocy's ingredient form; "round up" only where the server knows it (Grocy 4.4
 * and newer), older ones reject unknown columns.
 */
internal fun RecipeIngredient.toJson(): JsonObject = buildJsonObject {
    put("recipe_id", recipeId)
    put("product_id", productId)
    put("amount", amount)
    quId?.let { put("qu_id", it) }
    put("note", note.orEmpty())
    put("only_check_single_unit_in_stock", flag(onlyCheckSingleUnitInStock))
    put("ingredient_group", ingredientGroup.orEmpty())
    put("not_check_stock_fulfillment", flag(notCheckStockFulfillment))
    put("variable_amount", variableAmount.orEmpty())
    put("price_factor", priceFactor)
    roundUp?.let { put("round_up", flag(it)) }
}

/** A recipe included in another one, Grocy's `recipes_nestings`. */
@Serializable
internal data class RecipeNestingDto(
    val id: Int,
    @SerialName("recipe_id") val recipeId: FlexInt = null,
    @SerialName("includes_recipe_id") val includesRecipeId: FlexInt = null,
    val servings: FlexDouble = null,
) {
    fun toModel() = RecipeNesting(
        id = id,
        recipeId = recipeId ?: 0,
        includesRecipeId = includesRecipeId ?: 0,
        servings = servings ?: 1.0,
    )
}

internal fun RecipeNesting.toJson(): JsonObject = buildJsonObject {
    put("recipe_id", recipeId)
    put("includes_recipe_id", includesRecipeId)
    put("servings", servings)
}

/** A row of Grocy's view `recipes_pos_resolved`. */
@Serializable
internal data class ResolvedRecipeIngredientDto(
    @SerialName("recipe_id") val recipeId: Int,
    @SerialName("recipe_pos_id") val ingredientId: Int,
    @SerialName("product_id") val productId: FlexInt = null,
    @SerialName("recipe_amount") val recipeAmount: FlexDouble = null,
    @SerialName("stock_amount") val stockAmount: FlexDouble = null,
    @SerialName("need_fulfilled") val needFulfilled: FlexBoolean = null,
    @SerialName("missing_amount") val missingAmount: FlexDouble = null,
    @SerialName("amount_on_shopping_list") val amountOnShoppingList: FlexDouble = null,
    @SerialName("need_fulfilled_with_shopping_list")
    val needFulfilledWithShoppingList: FlexBoolean = null,
    @SerialName("qu_id") val quId: FlexInt = null,
    val costs: FlexDouble = null,
    @SerialName("is_nested_recipe_pos") val isNested: FlexBoolean = null,
    @SerialName("ingredient_group") val ingredientGroup: FlexString = null,
    @SerialName("product_group") val productGroup: FlexString = null,
    @SerialName("child_recipe_id") val childRecipeId: FlexInt = null,
    val note: FlexString = null,
    @SerialName("recipe_variable_amount") val variableAmount: FlexString = null,
    @SerialName("only_check_single_unit_in_stock")
    val onlyCheckSingleUnitInStock: FlexBoolean = null,
    val calories: FlexDouble = null,
    @SerialName("product_active") val productActive: FlexBoolean = null,
    @SerialName("due_score") val dueScore: FlexInt = null,
    @SerialName("product_id_effective") val productIdEffective: FlexInt = null,
) {
    fun toModel(): ResolvedRecipeIngredient {
        val product = productId ?: 0
        return ResolvedRecipeIngredient(
            recipeId = recipeId,
            ingredientId = ingredientId,
            productId = product,
            recipeAmount = recipeAmount ?: 0.0,
            stockAmount = stockAmount ?: 0.0,
            needFulfilled = needFulfilled ?: false,
            missingAmount = missingAmount ?: 0.0,
            amountOnShoppingList = amountOnShoppingList ?: 0.0,
            needFulfilledWithShoppingList = needFulfilledWithShoppingList ?: false,
            quId = quId,
            costs = costs ?: 0.0,
            isNested = isNested ?: false,
            ingredientGroup = ingredientGroup?.takeIf { it.isNotBlank() },
            productGroup = productGroup?.takeIf { it.isNotBlank() },
            childRecipeId = childRecipeId ?: recipeId,
            note = note?.takeIf { it.isNotBlank() },
            variableAmount = variableAmount?.takeIf { it.isNotBlank() },
            onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock ?: false,
            calories = calories ?: 0.0,
            productActive = productActive ?: true,
            dueScore = dueScore ?: 0,
            productIdEffective = productIdEffective ?: product,
        )
    }
}

@Serializable
internal data class MealPlanSectionDto(
    val id: Int,
    val name: FlexString = null,
    @SerialName("sort_number") val sortNumber: FlexInt = null,
) {
    fun toModel() = MealPlanSection(id = id, name = name.orEmpty(), sortNumber = sortNumber)
}
