package com.patrickzedler.grocy.feature.chores.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Grocy's "Chores overview", the list pane of a list-detail scene of its own. */
@Serializable
@SerialName("choresoverview")
data object ChoresOverviewRoute : Route

/**
 * Grocy's "Chore tracking", with chore [choreId] preselected if given. [inList]: beside the
 * chores overview where the window is wide enough; without, a screen of its own, e.g. what the
 * start screen opens in narrow windows.
 */
@Serializable
@SerialName("choretracking")
data class ChoreTrackingRoute(
    val choreId: Int? = null,
    val inList: Boolean = true,
) : DetailRoute

/** Grocy's "Chores journal", of chore [choreId] only if given, beside the chores overview. */
@Serializable
@SerialName("choresjournal")
data class ChoresJournalRoute(val choreId: Int? = null) : DetailRoute

val choresRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ChoresOverviewRoute::class, ChoresOverviewRoute.serializer())
        subclass(ChoreTrackingRoute::class, ChoreTrackingRoute.serializer())
        subclass(ChoresJournalRoute::class, ChoresJournalRoute.serializer())
    }
}
