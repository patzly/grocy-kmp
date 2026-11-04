package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSection
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationNavigationTest {

    @Test
    fun aNotificationOpensThePageOfItsFeature() {
        assertEquals(StockOverviewRoute, notificationRouteOf(NotificationKind.STOCK))
        assertEquals(ChoresOverviewRoute, notificationRouteOf(NotificationKind.CHORES))
        assertEquals(TasksRoute, notificationRouteOf(NotificationKind.TASKS))
        assertEquals(MealPlanRoute(), notificationRouteOf(NotificationKind.MEAL_PLAN))
    }

    private fun summary(showNames: Boolean) = NotificationSummary(
        kind = NotificationKind.STOCK,
        sections = listOf(
            NotificationSection(NotificationTopic.STOCK_OVERDUE, listOf("Milk", "Bread")),
            NotificationSection(NotificationTopic.STOCK_EXPIRED, listOf("Fish")),
        ),
        showNames = showNames,
    )

    @Test
    fun detailsListTheNamesBelowEachSentence() {
        assertEquals(
            "2 overdue\nMilk, Bread\n1 expired\nFish",
            notificationDetails(summary(showNames = true), listOf("2 overdue", "1 expired")),
        )
    }

    @Test
    fun withoutNamesOnlyTheSentencesRemain() {
        assertEquals(
            "2 overdue\n1 expired",
            notificationDetails(summary(showNames = false), listOf("2 overdue", "1 expired")),
        )
    }
}
