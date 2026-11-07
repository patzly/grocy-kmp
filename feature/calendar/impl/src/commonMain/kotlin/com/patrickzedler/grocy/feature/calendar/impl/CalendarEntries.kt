package com.patrickzedler.grocy.feature.calendar.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.core.productcard.ProductLinks
import com.patrickzedler.grocy.core.productcard.rememberStockPageActions
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.batteries.api.BatteryTrackingRoute
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.stock.api.StockEntriesRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalSummaryRoute
import com.patrickzedler.grocy.feature.stock.api.bookingRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute

/** Grocy's calendar, whose events open what they are about. */
fun EntryProviderScope<NavKey>.calendarEntries(
    dependencies: CalendarDependencies,
    navigator: Navigator,
) {
    entry<CalendarRoute> { route ->
        CalendarScreen(
            viewModel = viewModel {
                CalendarViewModel(
                    calendar = dependencies.calendarRepository,
                    masterData = dependencies.masterDataRepository,
                    options = dependencies.calendarOptions,
                    initialView = route.view,
                    initialDay = route.day?.let(::epochDayOf),
                )
            },
            stockPageActions = rememberStockPageActions(
                masterData = dependencies.masterDataRepository,
                stock = dependencies.stockRepository,
                shoppingLists = dependencies.shoppingListRepository,
                links = ProductLinks(
                    onEditClick = { navigator.navigate(ProductRoute(it)) },
                    onStockEntriesClick = { navigator.navigate(StockEntriesRoute(it)) },
                    onStockJournalClick = { navigator.navigate(StockJournalRoute(it)) },
                    onStockJournalSummaryClick = {
                        navigator.navigate(StockJournalSummaryRoute(it))
                    },
                    onBookingClick = { id, kind -> navigator.navigate(bookingRoute(kind, id)) },
                ),
            ),
            initialProductId = route.productId,
            onEventClick = { event -> navigator.navigate(routeOf(event)) },
            menuItems = appMenuItems(),
            onBackClick = navigator::goBack,
        )
    }
}

/** The product an event is about, whose overview the calendar shows over itself. */
internal val CalendarEvent.productId: Int?
    get() = objectId.takeIf {
        kind == CalendarEventKind.PRODUCT || kind == CalendarEventKind.MEAL_PLAN_PRODUCT
    }

/**
 * What [event] is about, unlike Grocy, which leads to the whole list it is part of: the task's
 * form, the tracking of the chore or battery, the recipe; a product's overview over the calendar's
 * day, what widgets open too. A meal plan note opens the meal plan's week.
 */
fun routeOf(event: CalendarEvent): Route {
    val day = isoDateOf(event.epochDay)
    val id = event.objectId
    return when (event.kind) {
        CalendarEventKind.PRODUCT, CalendarEventKind.MEAL_PLAN_PRODUCT ->
            CalendarRoute(CalendarView.DAY, day, productId = id)
        CalendarEventKind.TASK -> id?.let { TaskRoute(it, inList = false) } ?: TasksRoute
        CalendarEventKind.CHORE -> ChoreTrackingRoute(id, inList = false)
        CalendarEventKind.BATTERY -> BatteryTrackingRoute(id, inList = false)
        CalendarEventKind.MEAL_PLAN_RECIPE -> id?.let(::RecipeRoute) ?: MealPlanRoute(day)
        CalendarEventKind.MEAL_PLAN_NOTE -> MealPlanRoute(day)
    }
}
