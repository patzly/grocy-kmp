package com.patrickzedler.grocy.core.model.calendar

import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarTest {

    private val day = "2026-11-04"
    private val epochDay = epochDayOf(day)!!

    private fun stockAmount(productId: Int, due: String?, amount: Double = 1.0) = StockAmount(
        productId = productId,
        amount = amount,
        amountAggregated = amount,
        bestBeforeDate = due,
    )

    @Test
    fun productsInStockAreDueOnTheirDate() {
        val data = CalendarData(
            config = ServerConfig(),
            products = listOf(Product(id = 1, name = "Milk"), Product(id = 2, name = "Empty")),
            stock = listOf(stockAmount(1, day), stockAmount(2, day, amount = 0.0)),
        )

        assertEquals(
            listOf(CalendarEvent(CalendarEventKind.PRODUCT, "Milk", epochDay, objectId = 1)),
            calendarEventsOf(data),
        )
    }

    @Test
    fun withoutDueDateTrackingProductsAreNoEvents() {
        val data = CalendarData(
            config = ServerConfig(stockBestBeforeDateTracking = false),
            products = listOf(Product(id = 1, name = "Milk")),
            stock = listOf(stockAmount(1, day)),
        )

        assertTrue(calendarEventsOf(data).isEmpty())
    }

    @Test
    fun onlyTasksToDoWithADueDateAreEvents() {
        val data = CalendarData(
            config = ServerConfig(),
            tasks = listOf(
                Task(1, "Mow", dueDate = day),
                Task(2, "Done", dueDate = day, done = true),
                Task(3, "Someday", dueDate = null),
            ),
        )

        assertEquals(listOf("Mow"), calendarEventsOf(data).map { it.name })
    }

    @Test
    fun choresTellTheirTimeAndWhoTheyAreAssignedTo() {
        val data = CalendarData(
            config = ServerConfig(),
            chores = listOf(
                CurrentChore(
                    1,
                    "Vacuum",
                    nextEstimatedExecutionTime = "$day 18:30:00",
                    assignedUserId = 7,
                ),
                CurrentChore(
                    2,
                    "Mop",
                    nextEstimatedExecutionTime = "$day 23:59:59",
                    trackDateOnly = true,
                ),
                CurrentChore(3, "Unscheduled"),
            ),
            users = listOf(User(7, "ann", "Ann")),
        )

        assertEquals(
            listOf(
                CalendarEvent(CalendarEventKind.CHORE, "Mop", epochDay, objectId = 2),
                CalendarEvent(
                    CalendarEventKind.CHORE,
                    "Vacuum",
                    epochDay,
                    time = "18:30",
                    assignedTo = "Ann",
                    objectId = 1,
                ),
            ),
            calendarEventsOf(data),
        )
    }

    @Test
    fun theEndOfADayIsAWholeDay() {
        val data = CalendarData(
            config = ServerConfig(),
            currentBatteries = listOf(CurrentBattery(1, nextEstimatedChargeTime = "$day 23:59:59")),
            batteries = listOf(Battery(1, "Remote")),
        )

        assertNull(calendarEventsOf(data).single().time)
    }

    @Test
    fun whatNeverComesIsNoEvent() {
        val data = CalendarData(
            config = ServerConfig(),
            chores = listOf(
                CurrentChore(1, "Vacuum", nextEstimatedExecutionTime = "2999-12-31 23:59:59"),
            ),
            products = listOf(Product(id = 1, name = "Salt")),
            stock = listOf(stockAmount(1, "2999-12-31")),
        )

        assertTrue(calendarEventsOf(data).isEmpty())
    }

    @Test
    fun onlyActiveBatteriesAreEvents() {
        val data = CalendarData(
            config = ServerConfig(),
            currentBatteries = listOf(
                CurrentBattery(1, nextEstimatedChargeTime = "$day 08:00:00"),
                CurrentBattery(2, nextEstimatedChargeTime = "$day 08:00:00"),
                CurrentBattery(3, nextEstimatedChargeTime = "$day 08:00:00"),
            ),
            batteries = listOf(Battery(1, "Remote"), Battery(2, "Old", active = false)),
        )

        assertEquals(
            listOf(
                CalendarEvent(
                    CalendarEventKind.BATTERY,
                    "Remote",
                    epochDay,
                    time = "08:00",
                    objectId = 1,
                ),
            ),
            calendarEventsOf(data),
        )
    }

    @Test
    fun mealPlanEntriesTakeTheirSectionsNameAndTime() {
        val data = CalendarData(
            config = ServerConfig(),
            mealPlan = listOf(
                MealPlanEntry(1, day, MealPlanEntryType.RECIPE, recipeId = 5, sectionId = 2),
                MealPlanEntry(2, day, MealPlanEntryType.NOTE, note = "Eat out"),
                MealPlanEntry(3, day, MealPlanEntryType.PRODUCT, productId = 1, sectionId = 3),
                MealPlanEntry(4, day, MealPlanEntryType.RECIPE, recipeId = 99),
            ),
            sections = listOf(
                MealPlanSection(MealPlanSection.DEFAULT_ID, "", null),
                MealPlanSection(2, "Dinner", 1, timeInfo = "18:00"),
                MealPlanSection(3, "Snack", 2),
            ),
            recipes = listOf(Recipe(id = 5, name = "Soup")),
            products = listOf(Product(id = 1, name = "Apple")),
        )

        assertEquals(
            listOf(
                CalendarEvent(CalendarEventKind.MEAL_PLAN_NOTE, "Eat out", epochDay),
                CalendarEvent(
                    CalendarEventKind.MEAL_PLAN_PRODUCT,
                    "Apple",
                    epochDay,
                    section = "Snack",
                    objectId = 1,
                ),
                CalendarEvent(
                    CalendarEventKind.MEAL_PLAN_RECIPE,
                    "Soup",
                    epochDay,
                    time = "18:00",
                    section = "Dinner",
                    objectId = 5,
                ),
            ),
            calendarEventsOf(data),
        )
    }

    @Test
    fun featuresTurnedOffHaveNoEvents() {
        val data = CalendarData(
            config = ServerConfig(tasks = false, recipesMealPlan = false),
            tasks = listOf(Task(1, "Mow", dueDate = day)),
            mealPlan = listOf(MealPlanEntry(2, day, MealPlanEntryType.NOTE, note = "Eat out")),
        )

        assertTrue(calendarEventsOf(data).isEmpty())
    }

    @Test
    fun eventsAreOrderedByDayWholeDaysFirstThenByTime() {
        val data = CalendarData(
            config = ServerConfig(),
            tasks = listOf(Task(1, "Next day", dueDate = "2026-11-05"), Task(2, "Task", dueDate = day)),
            chores = listOf(
                CurrentChore(1, "Late", nextEstimatedExecutionTime = "$day 20:00:00"),
                CurrentChore(2, "Early", nextEstimatedExecutionTime = "$day 07:00:00"),
            ),
        )

        assertEquals(listOf("Task", "Early", "Late", "Next day"), calendarEventsOf(data).map { it.name })
    }

    @Test
    fun aMonthIsShownInWholeWeeks() {
        // November 2026 starts on a Sunday and ends on a Monday
        val monday = calendarMonthWeeks(epochDay, firstDayOfWeek = 1).map(::isoDateOf)
        val sunday = calendarMonthWeeks(epochDay, firstDayOfWeek = 0).map(::isoDateOf)

        assertEquals("2026-10-26", monday.first())
        assertEquals("2026-11-30", monday.last())
        assertEquals(6, monday.size)
        assertEquals("2026-11-01", sunday.first())
        assertEquals("2026-11-29", sunday.last())
        assertEquals(5, sunday.size)
    }

    @Test
    fun theFirstDayOfTheWeekDecidesWhereAWeekStarts() {
        assertEquals("2026-11-02", isoDateOf(calendarWeekStart(epochDay, firstDayOfWeek = 1)))
        assertEquals("2026-11-01", isoDateOf(calendarWeekStart(epochDay, firstDayOfWeek = 0)))
        assertEquals("2026-10-31", isoDateOf(calendarWeekStart(epochDay, firstDayOfWeek = 6)))
    }

    @Test
    fun eachViewGoesOnByItsPeriod() {
        fun after(view: CalendarView, steps: Int) = isoDateOf(calendarDayAfter(view, epochDay, steps))

        assertEquals("2026-12-04", after(CalendarView.MONTH, 1))
        assertEquals("2026-10-04", after(CalendarView.MONTH, -1))
        assertEquals("2026-11-11", after(CalendarView.WEEK, 1))
        assertEquals("2026-10-28", after(CalendarView.LIST, -1))
        assertEquals("2026-11-05", after(CalendarView.DAY, 1))
    }

    @Test
    fun pagesCountThePeriodsBetweenTwoDays() {
        val target = epochDayOf("2027-01-02")!!

        assertEquals(59, calendarStepsBetween(CalendarView.DAY, epochDay, target, 1))
        assertEquals(8, calendarStepsBetween(CalendarView.WEEK, epochDay, target, 1))
        assertEquals(2, calendarStepsBetween(CalendarView.MONTH, epochDay, target, 1))
        assertEquals(-2, calendarStepsBetween(CalendarView.MONTH, target, epochDay, 1))
    }

    @Test
    fun theStoredViewIsReadBackAndTheListIsTheDefault() {
        assertEquals(CalendarView.WEEK, CalendarView.of("WEEK"))
        assertEquals(CalendarView.LIST, CalendarView.of(null))
        assertEquals(CalendarView.LIST, CalendarView.of("YEAR"))
        assertEquals("MONTH", CalendarView.MONTH.stored())
    }

    @Test
    fun colorsAreNormalizedAndOthersRefused() {
        assertEquals("#007bff", normalizedHexColor("#007BFF"))
        assertEquals("#28a745", normalizedHexColor(" 28a745 "))
        assertNull(normalizedHexColor("#12345"))
        assertNull(normalizedHexColor("#12345g"))
        assertNull(normalizedHexColor(""))
    }

    @Test
    fun colorsBecomeOpaqueValues() {
        assertEquals(0xFF007BFFL, argbOfHexColor("#007bff"))
        assertNull(argbOfHexColor("blue"))
    }
}
