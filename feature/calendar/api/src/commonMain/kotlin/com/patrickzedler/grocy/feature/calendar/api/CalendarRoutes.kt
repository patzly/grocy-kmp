package com.patrickzedler.grocy.feature.calendar.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Grocy's "Calendar", a screen of its own that uses the whole window like the meal plan; in
 * [view] on [day] ("YYYY-MM-DD") if given, e.g. from a widget, else today in the window's view.
 */
@Serializable
@SerialName("calendar")
data class CalendarRoute(
    val view: CalendarView? = null,
    val day: String? = null,
) : Route

val calendarRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CalendarRoute::class, CalendarRoute.serializer())
    }
}
