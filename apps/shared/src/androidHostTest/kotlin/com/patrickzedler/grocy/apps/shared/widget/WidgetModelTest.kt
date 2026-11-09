package com.patrickzedler.grocy.apps.shared.widget

import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.epochDayOf
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetModelTest {

    private val today = epochDayOf("2026-11-05")!!

    private fun event(day: String, name: String = "Event") =
        CalendarEvent(CalendarEventKind.TASK, name, epochDayOf(day)!!)

    @Test
    fun theTaskListShowsTasksToDoByDueDateWithTheUndatedLast() {
        val tasks = listOf(
            Task(1, "Someday"),
            Task(2, "Next week", dueDate = "2026-11-12"),
            Task(3, "Done", dueDate = "2026-11-01", done = true),
            Task(4, "Overdue", dueDate = "2026-11-01"),
        )

        val shown = widgetTasksOf(tasks, today)

        assertEquals(listOf("Overdue", "Next week", "Someday"), shown.map { it.name })
        assertTrue(shown[0].isOverdue)
        assertFalse(shown[1].isOverdue)
    }

    @Test
    fun theTaskListShowsAtMostItsLimit() {
        val tasks = (1..5).map { Task(it, "Task $it") }

        assertEquals(3, widgetTasksOf(tasks, today, limit = 3).size)
    }

    @Test
    fun theScheduleStartsTodayAlsoWithoutEvents() {
        val rows = scheduleRowsOf(listOf(event("2026-11-04"), event("2026-11-07")), today)

        assertEquals(
            listOf(
                ScheduleRow.Day(today, isToday = true),
                ScheduleRow.NothingToday,
                ScheduleRow.Day(epochDayOf("2026-11-07")!!, isToday = false),
                ScheduleRow.Event(event("2026-11-07")),
            ),
            rows,
        )
    }

    @Test
    fun theScheduleShowsTodaysEventsBelowTheTitleAndStopsAtItsLimit() {
        val events = listOf(
            event("2026-11-05", "A"),
            event("2026-11-05", "B"),
            event("2026-11-06", "C"),
        )

        val rows = scheduleRowsOf(events, today, limit = 2)

        assertEquals(
            listOf(
                ScheduleRow.Day(today, isToday = true),
                ScheduleRow.Event(events[0]),
                ScheduleRow.Event(events[1]),
            ),
            rows,
        )
    }

    @Test
    fun theMonthFillsItsWeeksWithTheDaysAround() {
        // November 2026 from Monday on: Oct 26 to Dec 6
        val events = listOf(event("2026-11-07"))

        val weeks = monthCellsOf(today, today, firstDayOfWeek = 1, events = events)


        assertEquals(6, weeks.size)
        assertEquals(epochDayOf("2026-10-26"), weeks.first().first().epochDay)
        assertEquals(epochDayOf("2026-12-06"), weeks.last().last().epochDay)
        assertTrue(weeks.first().first().isOutside)
        val days = weeks.flatten()
        assertEquals(listOf(today), days.filter { it.isToday }.map { it.epochDay })
        assertEquals(1, days.single { it.epochDay == epochDayOf("2026-11-07") }.events.size)
    }

    @Test
    fun aSteppedMonthHoldsForTheDayAndReturnsToTodaysAfterwards() {
        assertEquals(epochDayOf("2027-01-01"), shownMonthOf(offset = 2, steppedOn = today, today))
        val tomorrow = today + 1
        assertEquals(epochDayOf("2026-11-01"), shownMonthOf(offset = 2, steppedOn = today, tomorrow))
        assertEquals(epochDayOf("2026-11-01"), shownMonthOf(offset = 0, steppedOn = null, today))
    }

    @Test
    fun aDayHasAsManyPlacesAsFitUpToFour() {
        assertEquals(0, monthChipPlaces(cellHeight = 30f))
        assertEquals(2, monthChipPlaces(cellHeight = 60f))
        assertEquals(4, monthChipPlaces(cellHeight = 400f))
    }

    @Test
    fun aFullDayKeepsOnePlaceForTheDots() {
        assertEquals(2, shownMonthChips(count = 2, places = 2))
        assertEquals(1, shownMonthChips(count = 3, places = 2))
        assertEquals(0, shownMonthChips(count = 3, places = 0))
    }

    @Test
    fun thePickerShowsTasksAndEventsOfEveryKindAroundToday() {
        val names = SampleNames(
            "Mow", "Seeds", "Water", "Descale", "Milk", "Vacuum", "Pasta", "Lunch", "Smoke",
            "Groceries", "Bread", "Eggs", "Apples",
        )

        val data = sampleWidgetData(today, names, AppearanceSettings())

        assertTrue(data.isLoggedIn)
        assertEquals(4, widgetTasksOf(data.tasks, today).size)
        assertTrue(widgetTasksOf(data.tasks, today).first().isOverdue)
        assertEquals(
            CalendarEventKind.entries.toSet() - setOf(
                CalendarEventKind.MEAL_PLAN_NOTE,
                CalendarEventKind.MEAL_PLAN_PRODUCT,
            ),
            data.events.map { it.kind }.toSet(),
        )
        assertTrue(scheduleRowsOf(data.events, today)[1] is ScheduleRow.Event)
    }

    @Test
    fun thePickerShowsAShoppingListWithItsItems() {
        val names = SampleNames(
            "Mow", "Seeds", "Water", "Descale", "Milk", "Vacuum", "Pasta", "Lunch", "Smoke",
            "Groceries", "Bread", "Eggs", "Apples",
        )

        val data = sampleWidgetData(today, names, AppearanceSettings())

        assertEquals("Groceries", data.shopping.lists.single().name)
        assertEquals(
            listOf("Apples", "Bread", "Eggs", "Milk"),
            widgetShoppingItemsOf(
                data.shopping,
                SAMPLE_SHOPPING_LIST_ID,
                data.settings,
                "en",
                '.',
            ).map { it.name },
        )
    }

    @Test
    fun chipNamesNeverBreakAfterTheirFirstWord() {
        assertEquals("St. Martin's Day", unbroken("St. Martin's Day"))
    }

    @Test
    fun theNextMidnightIsInTheDevicesZone() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")

        val midnight = ZonedDateTime.of(2026, 11, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))

        assertEquals(midnight.toInstant().toEpochMilli(), nextMidnightMillis(today, berlin))
    }

    @Test
    fun manifestDeclaresTheWidgetsAndWhatKeepsThemCurrent() {
        val manifest = File("src/androidMain/AndroidManifest.xml").readText()

        listOf(
            "widget.TaskListWidgetReceiver",
            "widget.MonthWidgetReceiver",
            "widget.ScheduleWidgetReceiver",
            "widget.ShoppingListWidgetReceiver",
            "@xml/widget_shopping_info",
            "widget.ShoppingListWidgetConfigureActivity",
            "android.appwidget.action.APPWIDGET_CONFIGURE",
            "widget.WidgetDayReceiver",
            "@xml/widget_tasks_info",
            "@xml/widget_month_info",
            "@xml/widget_schedule_info",
            "android.appwidget.action.APPWIDGET_UPDATE",
            "android.intent.action.LOCALE_CHANGED",
        ).forEach { assertTrue(it in manifest, "$it is missing") }
    }

    @Test
    fun widgetsAreKeptCurrentByTheAppNotByTheSystemsTimer() {
        listOf("tasks", "month", "schedule", "shopping").forEach { name ->
            val info = File("src/androidMain/res/xml/widget_${name}_info.xml").readText()

            assertTrue("""android:updatePeriodMillis="0"""" in info, name)
        }
    }

    private fun shoppingItem(
        id: Int,
        productId: Int? = null,
        note: String? = null,
        amount: Double = 1.0,
        quId: Int? = null,
        listId: Int = 1,
        done: Boolean = false,
    ) = ShoppingListItem(id, listId, productId, note, amount, quId, done)

    private val milk = Product(id = 1, name = "Milk", quIdStock = 1)
    private val shopping = WidgetShopping(
        products = listOf(milk),
        quantityUnits = listOf(
            QuantityUnit(1, "Piece", namePlural = "Pieces"),
            QuantityUnit(2, "Pack", namePlural = "Packs"),
        ),
        conversions = listOf(ResolvedQuantityUnitConversion(1, 1, 2, 0.5)),
    )

    private fun shownItems(data: WidgetShopping, listId: Int = 1) =
        widgetShoppingItemsOf(data, listId, UserSettings(), "en", '.')

    @Test
    fun theShoppingWidgetShowsWhatIsLeftOnItsListByName() {
        val data = shopping.copy(
            items = listOf(
                shoppingItem(1, note = "Zucchini"),
                shoppingItem(2, productId = 1),
                shoppingItem(3, note = "Bought", done = true),
                shoppingItem(4, note = "Other list", listId = 2),
            ),
        )

        assertEquals(listOf("Milk", "Zucchini"), shownItems(data).map { it.name })
    }

    @Test
    fun anItemShowsItsAmountInItsUnitAndItsNote() {
        val data = shopping.copy(
            items = listOf(shoppingItem(1, 1, note = "Organic", amount = 4.0, quId = 2)),
        )

        assertEquals("2 Packs · Organic", shownItems(data).single().details)
    }

    @Test
    fun aNoteWithoutProductIsTheItemsNameAndHasNoAmount() {
        val data = shopping.copy(items = listOf(shoppingItem(1, note = "Candles", amount = 3.0)))

        val item = shownItems(data).single()

        assertEquals("Candles", item.name)
        assertEquals(null, item.details)
    }

    @Test
    fun anItemWhoseProductIsNotKnownYetWaitsForTheNextDownload() {
        val data = shopping.copy(items = listOf(shoppingItem(1, productId = 99, note = "Note")))

        assertTrue(shownItems(data).isEmpty())
    }

    @Test
    fun theChoiceOfAListNeedsALoginAndTheFeature() {
        val lists = listOf(ShoppingList(1, "Groceries"))

        assertEquals(ShoppingListChoice.LoggedOut, shoppingListChoiceOf(false, true, lists))
        assertEquals(ShoppingListChoice.NotAvailable, shoppingListChoiceOf(true, false, lists))
        assertEquals(ShoppingListChoice.Lists(lists), shoppingListChoiceOf(true, true, lists))
    }

    @Test
    fun theShoppingWidgetAsksForItsListAndCanAskAgain() {
        val info = File("src/androidMain/res/xml/widget_shopping_info.xml").readText()

        assertTrue("ShoppingListWidgetConfigureActivity" in info)
        assertTrue("""android:widgetFeatures="reconfigurable"""" in info)
        assertFalse("configuration_optional" in info)
    }
}
