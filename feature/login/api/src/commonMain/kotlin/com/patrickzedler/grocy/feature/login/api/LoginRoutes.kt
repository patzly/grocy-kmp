package com.patrickzedler.grocy.feature.login.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object LoginChoiceRoute : Route

val loginRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(LoginChoiceRoute::class, LoginChoiceRoute.serializer())
    }
}
