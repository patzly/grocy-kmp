package com.patrickzedler.grocy.core.model.recipes

import com.patrickzedler.grocy.core.model.text.weekdayOf
import com.patrickzedler.grocy.core.model.text.yearWeekOf

/** What a meal plan entry plans: a recipe, a product or a note, Grocy's `type`. */
enum class MealPlanEntryType(val grocyName: String) {
    RECIPE("recipe"),
    PRODUCT("product"),
    NOTE("note"),
    ;

    companion object {
        /** Grocy's type [name]; entries of older versions have none and are recipes. */
        fun of(name: String?): MealPlanEntryType =
            entries.firstOrNull { it.grocyName == name } ?: RECIPE
    }
}

/**
 * An entry of Grocy's meal plan (`meal_plan`) on [day] ("YYYY-MM-DD"): [recipeServings] of a
 * recipe, [productAmount] (in the product's stock unit, entered in [productQuId]) of a product
 * or a [note], in section [sectionId], [done] once crossed off.
 */
data class MealPlanEntry(
    val id: Int,
    val day: String,
    val type: MealPlanEntryType,
    val recipeId: Int? = null,
    val recipeServings: Double = 1.0,
    val note: String? = null,
    val productId: Int? = null,
    val productAmount: Double = 0.0,
    val productQuId: Int? = null,
    val sectionId: Int = MealPlanSection.DEFAULT_ID,
    val done: Boolean = false,
)

/**
 * The kinds of recipes Grocy keeps for its meal plan by itself, computed like every recipe:
 * one per day ("YYYY-MM-DD"), one per week ("YYYY-WW") and a shadow recipe per planned recipe
 * ("YYYY-MM-DD#<entry id>") at the entry's servings.
 */
enum class MealPlanRecipeType(val grocyName: String) {
    DAY("mealplan-day"),
    WEEK("mealplan-week"),
    SHADOW("mealplan-shadow"),
    ;

    companion object {
        fun of(name: String?): MealPlanRecipeType? = entries.firstOrNull { it.grocyName == name }
    }
}

/**
 * One of Grocy's own recipes of the meal plan with what the server computed for it, null until
 * it computed anything.
 */
data class MealPlanRecipe(
    val id: Int,
    val type: MealPlanRecipeType,
    val name: String,
    val fulfillment: RecipeFulfillment? = null,
)

/** Grocy's name of the shadow recipe of the recipe entry [entryId] on [day]. */
fun mealPlanShadowRecipeName(day: String, entryId: Int): String = "$day#$entryId"

/**
 * Grocy's name of the week recipe of [epochDay]: SQLite's `%Y-%W`, weeks starting on Monday,
 * whatever day the meal plan starts its weeks with.
 */
fun mealPlanWeekRecipeName(epochDay: Long): String = yearWeekOf(epochDay).trimStart('0')

/**
 * The first day of the week [epochDay] lies in, like Grocy's meal plan: its own setting
 * [mealPlanFirstDay] (0 Sunday to 6 Saturday, -1 today's weekday [today]), else the calendar's
 * [calendarFirstDay], else the language's [localeFirstDay].
 */
fun mealPlanWeekStart(
    epochDay: Long,
    mealPlanFirstDay: Int?,
    calendarFirstDay: Int?,
    localeFirstDay: Int,
    today: Long,
): Long {
    val firstDay = when (mealPlanFirstDay) {
        null -> calendarFirstDay ?: localeFirstDay
        TODAY_FIRST_DAY -> weekdayOf(today)
        else -> mealPlanFirstDay
    }
    return epochDay - (weekdayOf(epochDay) - firstDay).mod(DAYS_PER_WEEK)
}

/** Grocy's `MEAL_PLAN_FIRST_DAY_OF_WEEK` that starts every week with today's weekday. */
const val TODAY_FIRST_DAY = -1

/** Days a week of the meal plan shows. */
const val DAYS_PER_WEEK = 7
