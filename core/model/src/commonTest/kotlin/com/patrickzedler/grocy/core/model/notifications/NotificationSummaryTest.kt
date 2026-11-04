package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.stockSummaryOf
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationSummaryTest {

    private val day = "2026-11-04"
    private val today = epochDayOf(day)!!
    private val all = NotificationSettings()

    private fun amount(productId: Int, due: String?, amount: Double = 1.0) = StockAmount(
        productId = productId,
        amount = amount,
        amountAggregated = amount,
        bestBeforeDate = due,
    )

    private fun product(id: Int, name: String, dueType: DueType = DueType.BEST_BEFORE) =
        Product(id = id, name = name, dueType = dueType)

    private fun stockSections(
        stock: List<StockAmount>,
        products: List<Product>,
        info: List<ProductStockInfo> = emptyList(),
        config: ServerConfig = ServerConfig(),
        settings: NotificationKindSettings = all[NotificationKind.STOCK],
    ) = stockNotificationSections(products, stock, info, config, settings, 5, today)

    @Test
    fun stockTellsEachStatusWithItsProducts() {
        val products = listOf(
            product(1, "Milk"),
            product(2, "Bread"),
            product(3, "Fish", DueType.EXPIRATION),
            product(4, "Apple"),
            product(5, "Salt"),
        )
        val stock = listOf(
            amount(1, "2026-11-06"),
            amount(4, "2026-11-05"),
            amount(2, "2026-11-01"),
            amount(3, "2026-11-01"),
            amount(5, "2027-01-01"),
        )

        val sections = stockSections(stock, products)

        assertEquals(
            listOf(
                NotificationSection(NotificationTopic.STOCK_DUE_SOON, listOf("Apple", "Milk")),
                NotificationSection(NotificationTopic.STOCK_OVERDUE, listOf("Bread")),
                NotificationSection(NotificationTopic.STOCK_EXPIRED, listOf("Fish")),
            ),
            sections,
        )
    }

    @Test
    fun stockCountsLikeTheStartScreen() {
        val products = listOf(
            product(1, "Milk"),
            product(2, "Hidden").copy(hideOnStockOverview = true),
            product(3, "Empty"),
            product(4, "Never"),
        )
        val stock = listOf(
            amount(1, "2026-11-01"),
            amount(2, "2026-11-01"),
            amount(3, "2026-11-01", amount = 0.0),
            amount(4, StockEntry.NEVER_OVERDUE),
        )
        val summary = stockSummaryOf(products, stock, emptyList(), ServerConfig(), 5, today)

        val sections = stockSections(stock, products)

        assertEquals(summary.overdueCount, sections.single().names.size)
        assertEquals(listOf("Milk"), sections.single().names)
    }

    @Test
    fun stockTellsWhatIsBelowItsMinStockAmount() {
        val info = listOf(
            ProductStockInfo(productId = 1, amountMissing = 2.0),
            ProductStockInfo(productId = 2),
        )
        val products = listOf(product(1, "Milk"), product(2, "Bread"))

        val sections = stockSections(emptyList(), products, info)

        assertEquals(
            listOf(NotificationSection(NotificationTopic.STOCK_BELOW_MIN, listOf("Milk"))),
            sections,
        )
    }

    @Test
    fun onlyChosenTopicsAreTold() {
        val settings = all[NotificationKind.STOCK].copy(
            topics = setOf(NotificationTopic.STOCK_EXPIRED),
        )
        val products = listOf(product(1, "Milk"))

        val sections = stockSections(listOf(amount(1, "2026-11-01")), products, settings = settings)

        assertTrue(sections.isEmpty())
    }

    @Test
    fun withoutDueDateTrackingStockTellsNoDueProducts() {
        val config = ServerConfig(stockBestBeforeDateTracking = false)
        val products = listOf(product(1, "Milk"))

        val sections = stockSections(listOf(amount(1, "2026-11-01")), products, config = config)

        assertTrue(sections.isEmpty())
    }

    private val chores = listOf(
        CurrentChore(1, "Vacuum", nextEstimatedExecutionTime = "2026-11-03 10:00:00"),
        CurrentChore(
            2,
            "Mop",
            nextEstimatedExecutionTime = "2026-11-04 18:00:00",
            assignedUserId = 7,
        ),
        CurrentChore(3, "Windows", nextEstimatedExecutionTime = "2026-11-06 10:00:00"),
        CurrentChore(4, "Someday"),
    )

    private fun choreSections(settings: NotificationKindSettings) = choresNotificationSections(
        chores = chores,
        settings = settings,
        dueSoonDays = 5,
        now = "$day 09:00:00",
        currentUserId = 7,
    )

    @Test
    fun choresDueSoonAreOffByDefault() {
        assertEquals(
            listOf(
                NotificationSection(NotificationTopic.CHORES_OVERDUE, listOf("Vacuum")),
                NotificationSection(NotificationTopic.CHORES_DUE_TODAY, listOf("Mop")),
            ),
            choreSections(all[NotificationKind.CHORES]),
        )
    }

    @Test
    fun choresCanTellOnlyWhatIsAssigned() {
        val settings = all[NotificationKind.CHORES].copy(onlyMine = true)

        assertEquals(
            listOf(NotificationSection(NotificationTopic.CHORES_DUE_TODAY, listOf("Mop"))),
            choreSections(settings),
        )
    }

    @Test
    fun choresDueSoonCountTodayLikeGrocy() {
        val settings = all[NotificationKind.CHORES].copy(
            topics = setOf(NotificationTopic.CHORES_DUE_SOON),
        )

        assertEquals(
            listOf(
                NotificationSection(NotificationTopic.CHORES_DUE_SOON, listOf("Mop", "Windows")),
            ),
            choreSections(settings),
        )
    }

    @Test
    fun tasksLeaveOutDoneOnesAndThoseOfOthers() {
        val tasks = listOf(
            Task(1, "Mow", dueDate = "2026-11-01", assignedToUserId = 7),
            Task(2, "Done", dueDate = "2026-11-01", done = true, assignedToUserId = 7),
            Task(3, "Other", dueDate = day, assignedToUserId = 8),
            Task(4, "Free", dueDate = null),
        )
        val settings = all[NotificationKind.TASKS].copy(onlyMine = true)

        val sections = tasksNotificationSections(tasks, settings, 5, today, currentUserId = 7)

        assertEquals(
            listOf(NotificationSection(NotificationTopic.TASKS_OVERDUE, listOf("Mow"))),
            sections,
        )
    }

    @Test
    fun tasksDueSoonCountTodayLikeGrocy() {
        val tasks = listOf(
            Task(1, "Today", dueDate = day),
            Task(2, "Soon", dueDate = "2026-11-06"),
            Task(3, "Later", dueDate = "2026-12-24"),
        )
        val settings = all[NotificationKind.TASKS].copy(
            topics = setOf(NotificationTopic.TASKS_DUE_SOON),
        )

        val sections = tasksNotificationSections(tasks, settings, 5, today, currentUserId = null)

        assertEquals(listOf("Today", "Soon"), sections.single().names)
    }

    @Test
    fun mealPlanTellsWhatIsLeftToday() {
        val entries = listOf(
            MealPlanEntry(1, day, MealPlanEntryType.RECIPE, recipeId = 1, sectionId = 2),
            MealPlanEntry(2, day, MealPlanEntryType.NOTE, note = "Eat out", sectionId = 1),
            MealPlanEntry(3, day, MealPlanEntryType.PRODUCT, productId = 1, done = true),
            MealPlanEntry(4, "2026-11-05", MealPlanEntryType.RECIPE, recipeId = 1),
        )
        val sections = listOf(
            MealPlanSection(1, "Lunch", 1),
            MealPlanSection(2, "Dinner", 2),
        )

        val told = mealPlanNotificationSections(
            entries = entries,
            sections = sections,
            recipes = listOf(Recipe(id = 1, name = "Soup")),
            products = listOf(product(1, "Milk")),
            settings = all[NotificationKind.MEAL_PLAN],
            today = today,
        )

        assertEquals(
            listOf(
                NotificationSection(NotificationTopic.MEAL_PLAN_TODAY, listOf("Eat out", "Soup")),
            ),
            told,
        )
    }

    @Test
    fun batteriesAreNamedLikeGrocysFilterAndDueSoonIsOffByDefault() {
        val settings = NotificationSettings()[NotificationKind.BATTERIES]
        val current = listOf(
            CurrentBattery(1, nextEstimatedChargeTime = "2026-11-01 08:00:00"),
            CurrentBattery(2, nextEstimatedChargeTime = "2026-11-04 20:00:00"),
            CurrentBattery(3, nextEstimatedChargeTime = "2026-11-06 08:00:00"),
            CurrentBattery(4),
            // Not known yet, it has no name to tell
            CurrentBattery(5, nextEstimatedChargeTime = "2026-11-01 08:00:00"),
        )
        val batteries = listOf(
            Battery(1, "Remote"),
            Battery(2, "Scale"),
            Battery(3, "Torch"),
            Battery(4, "Spare"),
        )

        val sections = batteriesNotificationSections(
            current = current,
            batteries = batteries,
            settings = settings,
            dueSoonDays = 5,
            now = "2026-11-04 12:00:00",
        )
        val withDueSoon = batteriesNotificationSections(
            current = current,
            batteries = batteries,
            settings = settings.copy(topics = NotificationKind.BATTERIES.topics.toSet()),
            dueSoonDays = 5,
            now = "2026-11-04 12:00:00",
        )

        assertEquals(
            listOf(
                NotificationSection(NotificationTopic.BATTERIES_OVERDUE, listOf("Remote")),
                NotificationSection(NotificationTopic.BATTERIES_DUE_TODAY, listOf("Scale")),
            ),
            sections,
        )
        assertEquals(
            listOf("Scale", "Torch"),
            withDueSoon.single { it.topic == NotificationTopic.BATTERIES_DUE_SOON }.names,
        )
    }
}
