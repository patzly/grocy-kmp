package com.patrickzedler.grocy.feature.settings.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
@SerialName("settings")
data object SettingsRoute : Route

@Serializable
@SerialName("settings/general")
data object GeneralSettingsRoute : Route

/** Recent errors of the app: logcat on Android, the errors the web app recorded. */
@Serializable
@SerialName("settings/log")
data object LogRoute : Route

val settingsRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsRoute::class, SettingsRoute.serializer())
        subclass(GeneralSettingsRoute::class, GeneralSettingsRoute.serializer())
        subclass(LogRoute::class, LogRoute.serializer())
    }
}
