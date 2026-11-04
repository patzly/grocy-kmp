package com.patrickzedler.grocy.feature.equipment.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Grocy's equipment, the list pane of a list-detail scene of its own. */
@Serializable
@SerialName("equipment")
data object EquipmentRoute : Route

/**
 * What Grocy's equipment page shows of piece [id]: manual, notes and files. [inList]: beside the
 * list where the window is wide enough; without, a screen of its own, Grocy's "Expand to
 * fullscreen".
 */
@Serializable
@SerialName("equipment/details")
data class EquipmentDetailRoute(
    val id: Int,
    val inList: Boolean = true,
) : DetailRoute

/** Grocy's equipment form, of a new piece without [id], beside the list. */
@Serializable
@SerialName("equipment/form")
data class EquipmentFormRoute(val id: Int? = null) : DetailRoute

val equipmentRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(EquipmentRoute::class, EquipmentRoute.serializer())
        subclass(EquipmentDetailRoute::class, EquipmentDetailRoute.serializer())
        subclass(EquipmentFormRoute::class, EquipmentFormRoute.serializer())
    }
}
