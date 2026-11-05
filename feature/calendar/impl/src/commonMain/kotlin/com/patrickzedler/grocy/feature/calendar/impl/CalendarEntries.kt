package com.patrickzedler.grocy.feature.calendar.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute

/** Grocy's calendar, whose events lead to the pages Grocy links them to. */
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
                    initialView = route.view,
                    initialDay = route.day?.let(::epochDayOf),
                )
            },
            onEventClick = { event -> navigator.navigate(routeOf(event)) },
            menuItems = appMenuItems(),
            onBackClick = navigator::goBack,
        )
    }
}

/** Where Grocy's calendar leads for [event], also from the widgets. */
fun routeOf(event: CalendarEvent): Route = when (event.kind) {
    CalendarEventKind.PRODUCT -> StockOverviewRoute
    CalendarEventKind.TASK -> TasksRoute
    CalendarEventKind.CHORE -> ChoresOverviewRoute
    CalendarEventKind.BATTERY -> BatteriesOverviewRoute
    CalendarEventKind.MEAL_PLAN_RECIPE -> event.recipeId?.let(::RecipeRoute)
        ?: MealPlanRoute(isoDateOf(event.epochDay))
    CalendarEventKind.MEAL_PLAN_NOTE,
    CalendarEventKind.MEAL_PLAN_PRODUCT,
    -> MealPlanRoute(isoDateOf(event.epochDay))
}
