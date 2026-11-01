package com.patrickzedler.grocy.core.model.recipes

import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MealPlanTest {

    // Wednesday, 2026-11-04
    private val wednesday = epochDayOf("2026-11-04")!!

    private fun start(
        mealPlan: Int?,
        calendar: Int? = null,
        locale: Int = 1,
        today: Long = wednesday,
    ) = isoDateOf(mealPlanWeekStart(wednesday, mealPlan, calendar, locale, today))

    @Test
    fun weeksStartWithTheMealPlansFirstDay() {
        assertEquals("2026-11-02", start(mealPlan = 1))
        assertEquals("2026-11-01", start(mealPlan = 0))
        assertEquals("2026-11-04", start(mealPlan = 3))
        assertEquals("2026-10-29", start(mealPlan = 4))
    }

    @Test
    fun withoutItsOwnSettingTheCalendarsThenTheLanguagesFirstDayCounts() {
        assertEquals("2026-11-01", start(mealPlan = null, calendar = 0, locale = 1))
        assertEquals("2026-11-02", start(mealPlan = null, calendar = null, locale = 1))
        assertEquals("2026-10-31", start(mealPlan = null, calendar = null, locale = 6))
    }

    @Test
    fun todaysWeekdayStartsTheWeeksWithMinusOne() {
        val friday = epochDayOf("2026-11-06")!!

        assertEquals("2026-11-04", start(mealPlan = TODAY_FIRST_DAY, today = wednesday))
        assertEquals("2026-10-30", start(mealPlan = TODAY_FIRST_DAY, today = friday))
    }

    @Test
    fun internalRecipesAreNamedLikeGrocysTriggersNameThem() {
        assertEquals("2026-44", mealPlanWeekRecipeName(wednesday))
        assertEquals("2026-11-04#7", mealPlanShadowRecipeName("2026-11-04", 7))
    }

    @Test
    fun entriesOfOlderGrocyVersionsAreRecipes() {
        assertEquals(MealPlanEntryType.NOTE, MealPlanEntryType.of("note"))
        assertEquals(MealPlanEntryType.RECIPE, MealPlanEntryType.of(null))
        assertEquals(MealPlanRecipeType.SHADOW, MealPlanRecipeType.of("mealplan-shadow"))
        assertNull(MealPlanRecipeType.of("normal"))
    }
}
