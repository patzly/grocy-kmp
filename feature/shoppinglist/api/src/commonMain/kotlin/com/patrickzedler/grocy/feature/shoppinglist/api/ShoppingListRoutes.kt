package com.patrickzedler.grocy.feature.shoppinglist.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Grocy's "Shopping list", the list [listId] (the first one without). */
@Serializable
@SerialName("shoppinglist")
data class ShoppingListRoute(val listId: Int? = null) : Route

/**
 * Grocy's shopping list item form: edits item [id], or creates an item on list [listId] (the
 * first one without) with product [productId] preselected if given. [inList]: beside its list
 * where the window is wide enough; without, a screen of its own, e.g. the start screen's quick
 * add in narrow windows.
 */
@Serializable
@SerialName("shoppinglist/item")
data class ShoppingListItemRoute(
    val id: Int? = null,
    val listId: Int? = null,
    val productId: Int? = null,
    val inList: Boolean = true,
) : DetailRoute

/** Grocy's shopping list form: renames list [id], or creates a list without. */
@Serializable
@SerialName("shoppinglist/list")
data class ShoppingListFormRoute(val id: Int? = null) : DetailRoute

/**
 * The shopping mode: the shown shopping list in large type for the store, checked off with a
 * tap and synced while people shop together; a screen of its own.
 */
@Serializable
@SerialName("shoppinglist/shopping-mode")
data object ShoppingModeRoute : Route

val shoppingListRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ShoppingListRoute::class, ShoppingListRoute.serializer())
        subclass(ShoppingListItemRoute::class, ShoppingListItemRoute.serializer())
        subclass(ShoppingListFormRoute::class, ShoppingListFormRoute.serializer())
        subclass(ShoppingModeRoute::class, ShoppingModeRoute.serializer())
    }
}
