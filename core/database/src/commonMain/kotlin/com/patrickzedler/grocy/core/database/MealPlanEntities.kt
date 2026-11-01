package com.patrickzedler.grocy.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** An entry of Grocy's meal plan; [type] is Grocy's ("recipe", "product", "note"). */
@Entity(tableName = "meal_plan")
data class MealPlanEntryEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(index = true) val day: String,
    val type: String,
    @ColumnInfo(name = "recipe_id") val recipeId: Int?,
    @ColumnInfo(name = "recipe_servings") val recipeServings: Double,
    val note: String?,
    @ColumnInfo(name = "product_id") val productId: Int?,
    @ColumnInfo(name = "product_amount") val productAmount: Double,
    @ColumnInfo(name = "product_qu_id") val productQuId: Int?,
    @ColumnInfo(name = "section_id") val sectionId: Int,
    val done: Boolean,
)

@Entity(tableName = "meal_plan_sections")
data class MealPlanSectionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "sort_number") val sortNumber: Int?,
    @ColumnInfo(name = "time_info") val timeInfo: String?,
)

/**
 * One of the recipes Grocy keeps for its meal plan ([type] is Grocy's, e.g. "mealplan-day")
 * with what the server computed for it (`recipes_resolved`); [hasFulfillment] is false until
 * it computed anything.
 */
@Entity(tableName = "meal_plan_recipes")
data class MealPlanRecipeEntity(
    @PrimaryKey val id: Int,
    val type: String,
    @ColumnInfo(index = true) val name: String,
    @ColumnInfo(name = "has_fulfillment") val hasFulfillment: Boolean,
    @ColumnInfo(name = "need_fulfilled") val needFulfilled: Boolean,
    @ColumnInfo(name = "need_fulfilled_with_shopping_list")
    val needFulfilledWithShoppingList: Boolean,
    @ColumnInfo(name = "missing_products_count") val missingProductsCount: Int,
    val costs: Double,
    val calories: Double,
    @ColumnInfo(name = "prices_incomplete") val pricesIncomplete: Boolean,
)
