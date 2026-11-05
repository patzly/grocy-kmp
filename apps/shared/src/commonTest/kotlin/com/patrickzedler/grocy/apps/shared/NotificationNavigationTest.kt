package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class NotificationNavigationTest {

    @Test
    fun aNotificationOpensThePageOfItsFeature() {
        assertEquals(StockOverviewRoute, notificationRouteOf(NotificationKind.STOCK))
        assertEquals(ChoresOverviewRoute, notificationRouteOf(NotificationKind.CHORES))
        assertEquals(TasksRoute, notificationRouteOf(NotificationKind.TASKS))
        assertEquals(BatteriesOverviewRoute, notificationRouteOf(NotificationKind.BATTERIES))
        assertEquals(MealPlanRoute(), notificationRouteOf(NotificationKind.MEAL_PLAN))
    }

    private suspend fun names(vararg names: String) = notificationNames(
        names = names.toList(),
        join = { first, second -> "$first, $second" },
        and = { first, second -> "$first and $second" },
        more = { shown, rest -> "$shown and $rest more" },
    )

    @Test
    fun oneToThreeNamesAreListedCompletely() = runTest {
        assertEquals("Eggs", names("Eggs"))
        assertEquals("Eggs and Paprika", names("Eggs", "Paprika"))
        assertEquals("Eggs, Paprika and Cucumber", names("Eggs", "Paprika", "Cucumber"))
    }

    @Test
    fun moreThanThreeNamesTellHowManyAreLeft() = runTest {
        assertEquals(
            "Eggs, Paprika, Cucumber and 3 more",
            names("Eggs", "Paprika", "Cucumber", "Milk", "Bread", "Salt"),
        )
    }
}
