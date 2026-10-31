package com.patrickzedler.grocy.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "picture_file_name") val pictureFileName: String?,
    @ColumnInfo(name = "base_servings") val baseServings: Double,
    @ColumnInfo(name = "desired_servings") val desiredServings: Double,
    @ColumnInfo(name = "not_check_shoppinglist") val notCheckShoppingList: Boolean,
    @ColumnInfo(name = "product_id") val productId: Int?,
    /** Userfield values by name as JSON object. */
    val userfields: String,
)

/** What the server computed for a recipe at its desired servings (`recipes_resolved`). */
@Entity(tableName = "recipe_fulfillment")
data class RecipeFulfillmentEntity(
    @PrimaryKey @ColumnInfo(name = "recipe_id") val recipeId: Int,
    @ColumnInfo(name = "need_fulfilled") val needFulfilled: Boolean,
    @ColumnInfo(name = "need_fulfilled_with_shopping_list")
    val needFulfilledWithShoppingList: Boolean,
    @ColumnInfo(name = "missing_products_count") val missingProductsCount: Int,
    val costs: Double,
    @ColumnInfo(name = "costs_per_serving") val costsPerServing: Double,
    val calories: Double,
    @ColumnInfo(name = "due_score") val dueScore: Int,
    @ColumnInfo(name = "product_names") val productNames: String,
    @ColumnInfo(name = "prices_incomplete") val pricesIncomplete: Boolean,
)

@Entity(tableName = "recipe_ingredients")
data class RecipeIngredientEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "recipe_id", index = true) val recipeId: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    val amount: Double,
    @ColumnInfo(name = "qu_id") val quId: Int?,
    val note: String?,
    @ColumnInfo(name = "only_check_single_unit_in_stock") val onlyCheckSingleUnitInStock: Boolean,
    @ColumnInfo(name = "ingredient_group") val ingredientGroup: String?,
    @ColumnInfo(name = "not_check_stock_fulfillment") val notCheckStockFulfillment: Boolean,
    @ColumnInfo(name = "variable_amount") val variableAmount: String?,
    @ColumnInfo(name = "price_factor") val priceFactor: Double,
    @ColumnInfo(name = "round_up") val roundUp: Boolean?,
)

@Entity(tableName = "recipe_nestings")
data class RecipeNestingEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "recipe_id", index = true) val recipeId: Int,
    @ColumnInfo(name = "includes_recipe_id") val includesRecipeId: Int,
    val servings: Double,
)

/**
 * An ingredient as the server resolved it for a recipe (`recipes_pos_resolved`), one row per
 * recipe and ingredient, included recipes' ingredients too.
 */
@Entity(
    tableName = "recipe_ingredients_resolved",
    primaryKeys = ["recipe_id", "ingredient_id"],
)
data class ResolvedRecipeIngredientEntity(
    @ColumnInfo(name = "recipe_id") val recipeId: Int,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "recipe_amount") val recipeAmount: Double,
    @ColumnInfo(name = "stock_amount") val stockAmount: Double,
    @ColumnInfo(name = "need_fulfilled") val needFulfilled: Boolean,
    @ColumnInfo(name = "missing_amount") val missingAmount: Double,
    @ColumnInfo(name = "amount_on_shopping_list") val amountOnShoppingList: Double,
    @ColumnInfo(name = "need_fulfilled_with_shopping_list")
    val needFulfilledWithShoppingList: Boolean,
    @ColumnInfo(name = "qu_id") val quId: Int?,
    val costs: Double,
    @ColumnInfo(name = "is_nested") val isNested: Boolean,
    @ColumnInfo(name = "ingredient_group") val ingredientGroup: String?,
    @ColumnInfo(name = "product_group") val productGroup: String?,
    @ColumnInfo(name = "child_recipe_id") val childRecipeId: Int,
    val note: String?,
    @ColumnInfo(name = "variable_amount") val variableAmount: String?,
    @ColumnInfo(name = "only_check_single_unit_in_stock") val onlyCheckSingleUnitInStock: Boolean,
    val calories: Double,
    @ColumnInfo(name = "product_active") val productActive: Boolean,
    @ColumnInfo(name = "due_score") val dueScore: Int,
    @ColumnInfo(name = "product_id_effective") val productIdEffective: Int,
)

/** A recipe picture as downloaded, kept for offline use; Grocy names every new one anew. */
@Entity(tableName = "recipe_pictures")
class RecipePictureEntity(
    @PrimaryKey @ColumnInfo(name = "file_name") val fileName: String,
    val bytes: ByteArray,
)
