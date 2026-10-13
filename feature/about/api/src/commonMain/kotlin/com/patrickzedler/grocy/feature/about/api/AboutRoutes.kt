package com.patrickzedler.grocy.feature.about.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object AboutRoute : Route

val aboutRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(AboutRoute::class, AboutRoute.serializer())
    }
}
