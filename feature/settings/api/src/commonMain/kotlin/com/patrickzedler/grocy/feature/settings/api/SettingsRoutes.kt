package com.patrickzedler.grocy.feature.settings.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** The categories; wide windows show the chosen one's page beside them. */
@Serializable
@SerialName("settings")
data object SettingsRoute : Route

// The pages of the categories open as details of the settings

@Serializable
@SerialName("settings/general")
data object GeneralSettingsRoute : DetailRoute

@Serializable
@SerialName("settings/server")
data object ServerSettingsRoute : DetailRoute

/**
 * Grocy's settings, the pages of its "Settings" menu: the ones it stores per user on the
 * server, a page per area, then its user settings.
 */
@Serializable
@SerialName("settings/grocy")
data object GrocySettingsRoute : DetailRoute

/**
 * Grocy's stock settings of the user, "Presets for new products" first; [inSettings] when
 * opened from Grocy's settings, so it shows beside the categories in wide windows.
 * [backLeavesSettings] when another screen opened it there together with the categories and
 * Grocy's settings below it, so back returns to that screen in one step.
 */
@Serializable
@SerialName("settings/stock")
data class StockSettingsRoute(
    val inSettings: Boolean = false,
    val backLeavesSettings: Boolean = false,
) : DetailRoute

/** The other pages of Grocy's settings menu with settings of the user, by their feature. */
@Serializable
enum class GrocySettingsPage { SHOPPING_LIST, RECIPES, CHORES, TASKS, BATTERIES }

/** One of Grocy's [GrocySettingsPage]s, beside the categories like [StockSettingsRoute]. */
@Serializable
@SerialName("settings/grocy/page")
data class GrocySettingsPageRoute(val page: GrocySettingsPage) : DetailRoute

/** Grocy's own "User settings" page: the user's language on the server. */
@Serializable
@SerialName("settings/user")
data object UserSettingsRoute : DetailRoute

/** Recent errors of the app: logcat on Android, the errors the web app recorded. */
@Serializable
@SerialName("settings/log")
data object LogRoute : Route

val settingsRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsRoute::class, SettingsRoute.serializer())
        subclass(GeneralSettingsRoute::class, GeneralSettingsRoute.serializer())
        subclass(ServerSettingsRoute::class, ServerSettingsRoute.serializer())
        subclass(GrocySettingsRoute::class, GrocySettingsRoute.serializer())
        subclass(GrocySettingsPageRoute::class, GrocySettingsPageRoute.serializer())
        subclass(UserSettingsRoute::class, UserSettingsRoute.serializer())
        subclass(StockSettingsRoute::class, StockSettingsRoute.serializer())
        subclass(LogRoute::class, LogRoute.serializer())
    }
}
