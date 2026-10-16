package com.patrickzedler.grocy.feature.settings.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object SettingsRoute : Route

@Serializable
data object GeneralSettingsRoute : Route

/** Recent errors of the app (logcat), only on Android. */
@Serializable
data object LogRoute : Route

val settingsRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsRoute::class, SettingsRoute.serializer())
        subclass(GeneralSettingsRoute::class, GeneralSettingsRoute.serializer())
        subclass(LogRoute::class, LogRoute.serializer())
    }
}
