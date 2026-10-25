package com.patrickzedler.grocy.feature.settings.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** The categories, alone until one is chosen; wide windows then show its page beside them. */
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

/** Grocy's stock settings of the user, "Presets for new products" first. */
@Serializable
@SerialName("settings/stock")
data object StockSettingsRoute : Route

/** Recent errors of the app: logcat on Android, the errors the web app recorded. */
@Serializable
@SerialName("settings/log")
data object LogRoute : Route

val settingsRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsRoute::class, SettingsRoute.serializer())
        subclass(GeneralSettingsRoute::class, GeneralSettingsRoute.serializer())
        subclass(ServerSettingsRoute::class, ServerSettingsRoute.serializer())
        subclass(StockSettingsRoute::class, StockSettingsRoute.serializer())
        subclass(LogRoute::class, LogRoute.serializer())
    }
}
