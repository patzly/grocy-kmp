package com.patrickzedler.grocy.feature.batteries.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Grocy's "Batteries overview", the list pane of a list-detail scene of its own. */
@Serializable
@SerialName("batteriesoverview")
data object BatteriesOverviewRoute : Route

/**
 * Grocy's "Battery tracking", with battery [batteryId] preselected if given. [inList]: beside the
 * batteries overview where the window is wide enough; without, a screen of its own, e.g. what the
 * start screen opens in narrow windows.
 */
@Serializable
@SerialName("batterytracking")
data class BatteryTrackingRoute(
    val batteryId: Int? = null,
    val inList: Boolean = true,
) : DetailRoute

/** Grocy's "Batteries journal", of battery [batteryId] only if given, beside the overview. */
@Serializable
@SerialName("batteriesjournal")
data class BatteriesJournalRoute(val batteryId: Int? = null) : DetailRoute

val batteriesRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(BatteriesOverviewRoute::class, BatteriesOverviewRoute.serializer())
        subclass(BatteryTrackingRoute::class, BatteryTrackingRoute.serializer())
        subclass(BatteriesJournalRoute::class, BatteriesJournalRoute.serializer())
    }
}
