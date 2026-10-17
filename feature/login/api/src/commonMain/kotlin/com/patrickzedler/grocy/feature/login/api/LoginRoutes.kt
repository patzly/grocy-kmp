package com.patrickzedler.grocy.feature.login.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
@SerialName("login")
data object LoginChoiceRoute : Route

@Serializable
@SerialName("login/scan")
data object LoginScanRoute : Route

@Serializable
@SerialName("login/manual")
data object LoginManualRoute : Route

val loginRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(LoginChoiceRoute::class, LoginChoiceRoute.serializer())
        subclass(LoginScanRoute::class, LoginScanRoute.serializer())
        subclass(LoginManualRoute::class, LoginManualRoute.serializer())
    }
}
