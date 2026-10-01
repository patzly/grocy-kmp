package com.patrickzedler.grocy.feature.start.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object StartRoute : Route

val startRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(StartRoute::class, StartRoute.serializer())
    }
}