package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.PaneMargin
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipe
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipeType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.mealPlanShadowRecipeName
import com.patrickzedler.grocy.core.model.recipes.mealPlanWeekRecipeName
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf

/** What a planned entry shows besides its section, by its type. */
internal sealed interface MealPlanContent {

    /** [servings] of a recipe and what Grocy computed for them ([fulfillment] of its shadow). */
    data class Recipe(
        val recipeId: Int,
        val name: String,
        val pictureFileName: String?,
        val servings: Double,
        val shadowRecipeId: Int?,
        val fulfillment: RecipeFulfillment?,
    ) : MealPlanContent {
        /** Grocy's energy "per serving" of the entry. */
        val caloriesPerServing: Double?
            get() = fulfillment?.calories?.takeIf { servings > 0 }?.div(servings)
    }

    /**
     * [amount] of a product in its stock [unit], [enoughInStock] like Grocy compares it with the
     * stock (sub products included), [costs] by its last price and its [calories], null where
     * Grocy knows none.
     */
    data class Product(
        val productId: Int,
        val name: String,
        val pictureFileName: String?,
        val amount: Double,
        val unit: QuantityUnit?,
        val enoughInStock: Boolean,
        val costs: Double?,
        val calories: Double?,
    ) : MealPlanContent

    data class Note(val text: String) : MealPlanContent
}

/** A planned entry with its section, the default one for a section that no longer exists. */
internal data class MealPlanItem(
    val entry: MealPlanEntry,
    val section: MealPlanSection,
    val content: MealPlanContent,
) {
    val name: String
        get() = when (content) {
            is MealPlanContent.Recipe -> content.name
            is MealPlanContent.Product -> content.name
            is MealPlanContent.Note -> content.text
        }
}

/** A day of the shown week, [summary] what Grocy computed for the whole day. */
internal data class MealPlanDay(
    val epochDay: Long,
    val isToday: Boolean,
    val summary: RecipeFulfillment?,
    val items: List<MealPlanItem>,
) {
    val day: String
        get() = isoDateOf(epochDay)

    /** The day's items of [section], in the order they were planned. */
    fun itemsOf(section: MealPlanSection): List<MealPlanItem> =
        items.filter { it.section.id == section.id }
}

/**
 * A week of Grocy's meal plan from [start]: its days, the [sections] any of them uses (Grocy's
 * rows) in their order, and Grocy's [weekRecipe] with the week's costs, null while nothing is
 * planned. Grocy's weeks run from Monday to Sunday whatever day the meal plan starts with, so
 * that is the week of the shown week's middle day, which shares most days with it.
 */
internal data class MealPlanWeek(
    val start: Long,
    val days: List<MealPlanDay>,
    val sections: List<MealPlanSection>,
    val weekRecipe: MealPlanRecipe?,
) {
    val end: Long
        get() = start + DAYS_PER_WEEK - 1

    val isEmpty: Boolean
        get() = days.all { it.items.isEmpty() }
}

/** What the meal plan of a week is built from, all of it as last downloaded. */
internal class MealPlanData(
    val entries: List<MealPlanEntry>,
    val sections: List<MealPlanSection>,
    val mealPlanRecipes: List<MealPlanRecipe>,
    val recipes: List<Recipe>,
    val products: List<Product>,
    val units: List<QuantityUnit>,
    val stock: List<StockAmount>,
    val stockInfo: List<ProductStockInfo>,
)

/**
 * The week from [start] like Grocy's meal plan shows it: per day its entries in the order of
 * their sections, then as planned; recipes and products with what the server computed for
 * them (the shadow recipe of each planned recipe, the day and week recipes for the summaries).
 * Entries of deleted recipes or products are left out like Grocy does.
 */
internal fun buildMealPlanWeek(start: Long, today: Long, data: MealPlanData): MealPlanWeek {
    val days = (0 until DAYS_PER_WEEK).map { start + it }
    val dayNames = days.map { isoDateOf(it) }.toSet()
    val sectionsById = data.sections.associateBy { it.id }
    val defaultSection = sectionsById[MealPlanSection.DEFAULT_ID]
        ?: MealPlanSection(MealPlanSection.DEFAULT_ID, "", sortNumber = null)
    val sectionOrder = data.sections.withIndex().associate { (index, section) -> section.id to index }
    val recipesById = data.recipes.associateBy { it.id }
    val productsById = data.products.associateBy { it.id }
    val unitsById = data.units.associateBy { it.id }
    val stockById = data.stock.associateBy { it.productId }
    val pricesById = data.stockInfo.associate { it.productId to it.lastPrice }
    val internal = data.mealPlanRecipes.associateBy { it.type to it.name }

    fun contentOf(entry: MealPlanEntry): MealPlanContent? = when (entry.type) {
        MealPlanEntryType.RECIPE -> entry.recipeId?.let { recipesById[it] }?.let { recipe ->
            val shadow =
                internal[MealPlanRecipeType.SHADOW to mealPlanShadowRecipeName(entry.day, entry.id)]
            MealPlanContent.Recipe(
                recipeId = recipe.id,
                name = recipe.name,
                pictureFileName = recipe.pictureFileName,
                servings = entry.recipeServings,
                shadowRecipeId = shadow?.id,
                fulfillment = shadow?.fulfillment,
            )
        }
        MealPlanEntryType.PRODUCT -> entry.productId?.let { productsById[it] }?.let { product ->
            val amount = entry.productAmount
            MealPlanContent.Product(
                productId = product.id,
                name = product.name,
                pictureFileName = product.pictureFileName,
                amount = amount,
                unit = product.quIdStock?.let { unitsById[it] },
                enoughInStock = (stockById[product.id]?.amountAggregated ?: 0.0) >= amount,
                costs = pricesById[product.id]?.let { it * amount },
                calories = product.calories?.takeIf { it > 0 }?.let { it * amount },
            )
        }
        MealPlanEntryType.NOTE -> MealPlanContent.Note(entry.note.orEmpty())
    }

    val items = data.entries
        .filter { it.day in dayNames }
        .mapNotNull { entry ->
            contentOf(entry)?.let { content ->
                MealPlanItem(entry, sectionsById[entry.sectionId] ?: defaultSection, content)
            }
        }
        .sortedWith(
            compareBy<MealPlanItem> { sectionOrder[it.section.id] ?: -1 }.thenBy { it.entry.id },
        )
        .groupBy { it.entry.day }

    val usedSections = items.values.flatten().map { it.section }.distinctBy { it.id }
        .sortedBy { sectionOrder[it.id] ?: -1 }
    return MealPlanWeek(
        start = start,
        days = days.map { epochDay ->
            val day = isoDateOf(epochDay)
            MealPlanDay(
                epochDay = epochDay,
                isToday = epochDay == today,
                summary = internal[MealPlanRecipeType.DAY to day]?.fulfillment
                    ?.takeIf { items[day].orEmpty().isNotEmpty() },
                items = items[day].orEmpty(),
            )
        },
        sections = usedSections,
        weekRecipe = internal[
            MealPlanRecipeType.WEEK to mealPlanWeekRecipeName(start + DAYS_PER_WEEK / 2)
        ]?.takeIf { items.isNotEmpty() },
    )
}

/** The day [day] ("YYYY-MM-DD") points to, today for none or an invalid one. */
internal fun anchorDayOf(day: String?, today: Long): Long =
    day?.let { epochDayOf(it) } ?: today

/**
 * Whether a window [width] wide shows the week as Grocy's grid, a column per day and a row
 * per section, instead of the days below each other: once every day gets
 * [MinDayColumnWidth] beside the screen margins, and the pane's ones on both sides as
 * [isSinglePane].
 */
internal fun showsWeekGrid(width: Dp, isSinglePane: Boolean = false): Boolean {
    // A single pane keeps the screen's margin on both sides
    val paneWidth = if (isSinglePane) width - PaneMargin * 2 else width
    val content = paneWidth - GridMargins * 2
    return (content - DayColumnGap * (DAYS_PER_WEEK - 1)) / DAYS_PER_WEEK >= MinDayColumnWidth
}

// Room for a picture, a two line name and a chip of a day's card
internal val MinDayColumnWidth = 136.dp
internal val DayColumnGap = 8.dp
internal val GridMargins = 16.dp
