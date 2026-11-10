package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.model.start.StartLayout
import com.patrickzedler.grocy.core.model.start.StartSection
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.calendar_nothing_today
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.recipes_empty
import com.patrickzedler.grocy.core.resources.stock_overview_empty
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductsRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipesRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListItemRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.ConsumeRoute
import com.patrickzedler.grocy.feature.stock.api.InventoryRoute
import com.patrickzedler.grocy.feature.stock.api.PurchaseRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.stock.api.TransferRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartSectionTest {

    @Test
    fun sectionsWithoutDataSayWhatTheySayWhenEmpty() {
        assertEquals(Res.string.recipes_empty, placeholderOf(StartSection.RECIPES))
        assertEquals(Res.string.stock_overview_empty, placeholderOf(StartSection.STOCK_OVERVIEW))
        assertEquals(Res.string.objects_empty, placeholderOf(StartSection.TASKS))
        assertEquals(Res.string.calendar_nothing_today, placeholderOf(StartSection.CALENDAR))
    }

    @Test
    fun sectionsOpenTheirPage() {
        assertEquals(StockOverviewRoute, StartSection.STOCK_OVERVIEW.route)
        assertEquals(ShoppingListRoute(), StartSection.SHOPPING_LIST.route)
        assertEquals(MasterDataRoute, StartSection.MASTER_DATA.route)
        assertEquals(RecipesRoute(), StartSection.RECIPES.route)
        assertEquals(MealPlanRoute(), StartSection.MEAL_PLAN.route)
        assertEquals(ChoresOverviewRoute, StartSection.CHORES.route)
        assertEquals(TasksRoute, StartSection.TASKS.route)
        assertEquals(CalendarRoute(), StartSection.CALENDAR.route)
        // List-detail scenes of their own, never beside the start screen
        assertFalse(StartSection.SHOPPING_LIST.route is DetailRoute)
        assertFalse(StartSection.RECIPES.route is DetailRoute)
        assertFalse(StartSection.CHORES.route is DetailRoute)
        assertFalse(StartSection.TASKS.route is DetailRoute)
        // The meal plan needs the whole window
        assertFalse(StartSection.MEAL_PLAN.route is DetailRoute)
    }

    @Test
    fun theCalendarFollowsItsFeature() {
        assertFalse(StartSection.CALENDAR in availableSections(ServerConfig(calendar = false)))
    }

    @Test
    fun stockActionsOpenTheirPageBesideTheStartScreen() {
        assertEquals(PurchaseRoute(besideStart = true), StockAction.PURCHASE.route)
        assertEquals(ConsumeRoute(besideStart = true), StockAction.CONSUME.route)
        assertEquals(TransferRoute(besideStart = true), StockAction.TRANSFER.route)
        assertEquals(InventoryRoute(besideStart = true), StockAction.INVENTORY.route)
    }

    @Test
    fun addItemOpensTheShoppingListWithTheItemInWideWindows() {
        assertEquals(
            listOf(ShoppingListRoute(listId = 2), ShoppingListItemRoute(listId = 2)),
            shoppingListAddRoutes(listId = 2, isBesideDetail = true),
        )
    }

    @Test
    fun addItemOpensTheItemAloneInNarrowWindows() {
        assertEquals(
            listOf(ShoppingListItemRoute(listId = 2, inList = false)),
            shoppingListAddRoutes(listId = 2, isBesideDetail = false),
        )
    }

    @Test
    fun choreTrackingOpensBesideTheOverviewInWideWindows() {
        assertEquals(
            listOf(ChoresOverviewRoute, ChoreTrackingRoute()),
            choreTrackingRoutes(isBesideDetail = true),
        )
    }

    @Test
    fun choreTrackingOpensAloneInNarrowWindows() {
        assertEquals(
            listOf(ChoreTrackingRoute(inList = false)),
            choreTrackingRoutes(isBesideDetail = false),
        )
    }

    @Test
    fun creatingATaskOpensBesideTheTasksInWideWindows() {
        assertEquals(listOf(TasksRoute, TaskRoute()), taskCreateRoutes(isBesideDetail = true))
    }

    @Test
    fun creatingATaskOpensAloneInNarrowWindows() {
        assertEquals(
            listOf(TaskRoute(inList = false)),
            taskCreateRoutes(isBesideDetail = false),
        )
    }

    @Test
    fun shownPageMarksItsSection() {
        assertEquals(StartSection.STOCK_OVERVIEW, startSectionOf(StockOverviewRoute))
        assertEquals(StartSection.MASTER_DATA, startSectionOf(MasterDataRoute))
    }

    @Test
    fun batteriesAndEquipmentFollowTheTasksInTheirGroup() {
        val sections = StartLayout.Default.groups(ServerConfig())
            .first { StartSection.CHORES in it }

        assertEquals(
            listOf(
                StartSection.CHORES,
                StartSection.TASKS,
                StartSection.BATTERIES,
                StartSection.EQUIPMENT,
                StartSection.CALENDAR,
            ),
            sections,
        )
    }

    @Test
    fun stockSectionsComeFirstAndOnlyWithTheirFeatures() {
        assertEquals(
            listOf(
                StartSection.STOCK_OVERVIEW,
                StartSection.SHOPPING_LIST,
                StartSection.RECIPES,
                StartSection.MEAL_PLAN,
                StartSection.CHORES,
                StartSection.TASKS,
                StartSection.BATTERIES,
                StartSection.EQUIPMENT,
                StartSection.CALENDAR,
                StartSection.MASTER_DATA,
            ),
            availableSections(ServerConfig()),
        )
        assertEquals(
            listOf(
                StartSection.STOCK_OVERVIEW,
                StartSection.RECIPES,
                StartSection.MEAL_PLAN,
                StartSection.CHORES,
                StartSection.TASKS,
                StartSection.BATTERIES,
                StartSection.EQUIPMENT,
                StartSection.CALENDAR,
                StartSection.MASTER_DATA,
            ),
            availableSections(ServerConfig(shoppingList = false)),
        )
        assertEquals(
            listOf(
                StartSection.RECIPES,
                StartSection.MEAL_PLAN,
                StartSection.CHORES,
                StartSection.TASKS,
                StartSection.BATTERIES,
                StartSection.EQUIPMENT,
                StartSection.CALENDAR,
                StartSection.MASTER_DATA,
            ),
            availableSections(ServerConfig(stock = false)),
        )
        assertEquals(
            listOf(
                StartSection.STOCK_OVERVIEW,
                StartSection.SHOPPING_LIST,
                StartSection.RECIPES,
                StartSection.CHORES,
                StartSection.TASKS,
                StartSection.BATTERIES,
                StartSection.EQUIPMENT,
                StartSection.CALENDAR,
                StartSection.MASTER_DATA,
            ),
            availableSections(ServerConfig(recipesMealPlan = false)),
        )
        assertEquals(
            listOf(
                StartSection.STOCK_OVERVIEW,
                StartSection.SHOPPING_LIST,
                StartSection.CHORES,
                StartSection.TASKS,
                StartSection.BATTERIES,
                StartSection.EQUIPMENT,
                StartSection.CALENDAR,
                StartSection.MASTER_DATA,
            ),
            availableSections(ServerConfig(recipes = false)),
        )
        assertFalse(StartSection.CHORES in availableSections(ServerConfig(chores = false)))
    }

    @Test
    fun everySectionShowsUntilTheConfigurationIsRead() {
        assertEquals(StartSection.entries, availableSections(null))
    }

    @Test
    fun otherRoutesMarkNoSection() {
        // The start screen alone, or a list opened from a section's page
        assertNull(startSectionOf(StartRoute))
        assertNull(startSectionOf(ProductsRoute))
    }

    @Test
    fun tasksSectionNeedsItsFeature() {
        assertFalse(StartSection.TASKS in availableSections(ServerConfig(tasks = false)))
    }

    @Test
    fun aSingleSummaryLineIsCenteredAndAListOrButtonsStartAtTheTop() {
        assertFalse(startsAtTop(bullets = emptyList(), hasButtons = false))
        assertTrue(startsAtTop(bullets = listOf("3 overdue"), hasButtons = false))
        assertTrue(startsAtTop(bullets = emptyList(), hasButtons = true))
    }
}
