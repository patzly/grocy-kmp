package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipe
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipeType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MealPlanWeekTest {

    private val monday = epochDayOf("2026-11-02")!!
    private val default = MealPlanSection(MealPlanSection.DEFAULT_ID, "", -1)
    private val breakfast = MealPlanSection(1, "Breakfast", 1, "7:00")
    private val dinner = MealPlanSection(2, "Dinner", 2)
    private val piece = QuantityUnit(1, "Piece", "Pieces")

    private fun fulfillment(id: Int, costs: Double, calories: Double, fulfilled: Boolean = true) =
        RecipeFulfillment(
            recipeId = id,
            needFulfilled = fulfilled,
            needFulfilledWithShoppingList = fulfilled,
            missingProductsCount = if (fulfilled) 0 else 1,
            costs = costs,
            costsPerServing = costs,
            calories = calories,
            dueScore = 0,
            productNames = "",
            pricesIncomplete = false,
        )

    private fun entry(
        id: Int,
        day: String = "2026-11-02",
        type: MealPlanEntryType = MealPlanEntryType.RECIPE,
        sectionId: Int = MealPlanSection.DEFAULT_ID,
    ) = MealPlanEntry(
        id = id,
        day = day,
        type = type,
        recipeId = 1.takeIf { type == MealPlanEntryType.RECIPE },
        recipeServings = 2.0,
        note = "Eat out".takeIf { type == MealPlanEntryType.NOTE },
        productId = 1.takeIf { type == MealPlanEntryType.PRODUCT },
        productAmount = 3.0,
        sectionId = sectionId,
    )

    private fun week(
        entries: List<MealPlanEntry>,
        mealPlanRecipes: List<MealPlanRecipe> = emptyList(),
        recipes: List<Recipe> = listOf(Recipe(1, "Pancakes", pictureFileName = "p.jpg")),
        stock: List<StockAmount> = emptyList(),
        today: Long = monday + 2,
    ) = buildMealPlanWeek(
        start = monday,
        today = today,
        data = MealPlanData(
            entries = entries,
            sections = listOf(default, breakfast, dinner),
            mealPlanRecipes = mealPlanRecipes,
            recipes = recipes,
            products = listOf(Product(id = 1, name = "Milk", quIdStock = 1, calories = 50.0)),
            units = listOf(piece),
            stock = stock,
            stockInfo = listOf(ProductStockInfo(productId = 1, lastPrice = 0.5)),
        ),
    )

    @Test
    fun theWeekHasSevenDaysFromItsStartWithToday() {
        val week = week(entries = emptyList())

        assertEquals((0..6).map { monday + it }, week.days.map { it.epochDay })
        assertEquals(listOf(false, false, true, false, false, false, false), week.days.map { it.isToday })
        assertTrue(week.isEmpty)
        assertEquals(monday + 6, week.end)
    }

    @Test
    fun entriesOfOtherWeeksAreLeftOut() {
        val week = week(entries = listOf(entry(1, day = "2026-11-09"), entry(2, day = "2026-11-01")))

        assertTrue(week.isEmpty)
    }

    @Test
    fun aDaysEntriesComeInTheOrderOfTheirSectionsThenAsPlanned() {
        val week = week(
            entries = listOf(
                entry(1, sectionId = dinner.id),
                entry(2, sectionId = breakfast.id),
                entry(3),
                entry(4, sectionId = breakfast.id),
            ),
        )

        assertEquals(listOf(3, 2, 4, 1), week.days.first().items.map { it.entry.id })
        assertEquals(listOf(default, breakfast, dinner), week.sections)
    }

    @Test
    fun theGridsRowsAreTheSectionsTheWeekUses() {
        val week = week(entries = listOf(entry(1, sectionId = dinner.id)))

        assertEquals(listOf(dinner), week.sections)
        assertEquals(listOf(1), week.days.first().itemsOf(dinner).map { it.entry.id })
        assertTrue(week.days.first().itemsOf(breakfast).isEmpty())
    }

    @Test
    fun entriesOfADeletedSectionShowInTheDefaultOne() {
        val week = week(entries = listOf(entry(1, sectionId = 9)))

        assertEquals(default, week.days.first().items.single().section)
    }

    @Test
    fun aPlannedRecipeHasWhatGrocyComputedForItsShadowRecipe() {
        val week = week(
            entries = listOf(entry(5)),
            mealPlanRecipes = listOf(
                MealPlanRecipe(
                    id = -3,
                    type = MealPlanRecipeType.SHADOW,
                    name = "2026-11-02#5",
                    fulfillment = fulfillment(-3, costs = 4.0, calories = 700.0, fulfilled = false),
                ),
            ),
        )

        val content = assertIs<MealPlanContent.Recipe>(week.days.first().items.single().content)
        assertEquals("Pancakes", content.name)
        assertEquals("p.jpg", content.pictureFileName)
        assertEquals(-3, content.shadowRecipeId)
        assertEquals(4.0, content.fulfillment?.costs)
        // Grocy's energy per serving of the entry's two servings
        assertEquals(350.0, content.caloriesPerServing)
    }

    @Test
    fun aPlannedProductIsCheckedAgainstTheStockAndPricedByItsLastPrice() {
        val week = week(
            entries = listOf(entry(1, type = MealPlanEntryType.PRODUCT)),
            stock = listOf(StockAmount(productId = 1, amount = 2.0, amountAggregated = 2.0)),
        )

        val content = assertIs<MealPlanContent.Product>(week.days.first().items.single().content)
        assertEquals("Milk", content.name)
        assertEquals(piece, content.unit)
        assertFalse(content.enoughInStock)
        assertEquals(1.5, content.costs)
        assertEquals(150.0, content.calories)
    }

    @Test
    fun enoughStockCountsTheSubProductsToo() {
        val week = week(
            entries = listOf(entry(1, type = MealPlanEntryType.PRODUCT)),
            stock = listOf(StockAmount(productId = 1, amount = 0.0, amountAggregated = 3.0)),
        )

        val content = assertIs<MealPlanContent.Product>(week.days.first().items.single().content)
        assertTrue(content.enoughInStock)
    }

    @Test
    fun entriesOfDeletedRecipesAreLeftOutLikeGrocyDoes() {
        val week = week(
            entries = listOf(entry(1), entry(2, type = MealPlanEntryType.NOTE)),
            recipes = emptyList(),
        )

        val item = week.days.first().items.single()
        assertEquals("Eat out", item.name)
    }

    @Test
    fun daysAndTheWeekHaveGrocysSummariesWhileAnythingIsPlanned() {
        val recipes = listOf(
            MealPlanRecipe(-1, MealPlanRecipeType.WEEK, "2026-44", fulfillment(-1, 9.0, 900.0)),
            MealPlanRecipe(-2, MealPlanRecipeType.DAY, "2026-11-02", fulfillment(-2, 9.0, 900.0)),
            // An old day recipe whose entries are gone
            MealPlanRecipe(-4, MealPlanRecipeType.DAY, "2026-11-03", fulfillment(-4, 1.0, 1.0)),
        )

        val planned = week(entries = listOf(entry(1)), mealPlanRecipes = recipes)
        val empty = week(entries = emptyList(), mealPlanRecipes = recipes)

        assertEquals(9.0, planned.days[0].summary?.costs)
        assertNull(planned.days[1].summary)
        assertEquals(-1, planned.weekRecipe?.id)
        assertNull(empty.weekRecipe)
    }

    @Test
    fun weeksStartingOnSundayUseGrocysWeekOfTheirMiddleDay() {
        val sunday = epochDayOf("2026-11-01")!!
        val week = buildMealPlanWeek(
            start = sunday,
            today = sunday,
            data = MealPlanData(
                entries = listOf(MealPlanEntry(1, "2026-11-01", MealPlanEntryType.NOTE)),
                sections = listOf(default),
                mealPlanRecipes = listOf(
                    MealPlanRecipe(-5, MealPlanRecipeType.WEEK, "2026-43"),
                    MealPlanRecipe(-1, MealPlanRecipeType.WEEK, "2026-44"),
                ),
                recipes = emptyList(),
                products = emptyList(),
                units = emptyList(),
                stock = emptyList(),
                stockInfo = emptyList(),
            ),
        )

        // Six of its days lie in Grocy's week from Monday 2026-11-02
        assertEquals(-1, week.weekRecipe?.id)
    }

    @Test
    fun anInvalidDayShowsTodaysWeek() {
        assertEquals(monday, anchorDayOf("2026-11-02", today = 5))
        assertEquals(5, anchorDayOf("2.11.2026", today = 5))
        assertEquals(5, anchorDayOf(null, today = 5))
    }

    @Test
    fun theGridNeedsRoomForSevenDays() {
        // A phone, an unfolded foldable, a tablet held upright
        assertFalse(showsWeekGrid(412.dp))
        assertFalse(showsWeekGrid(840.dp))
        // A tablet in landscape, a desktop browser
        assertTrue(showsWeekGrid(1280.dp))
        assertTrue(showsWeekGrid(1920.dp))
    }

    @Test
    fun sectionsAreNamedWithTheirTime() {
        assertEquals("Breakfast · 7:00", sectionLabel(breakfast))
        assertEquals("Dinner", sectionLabel(dinner))
        assertNull(sectionLabel(default))
    }
}
