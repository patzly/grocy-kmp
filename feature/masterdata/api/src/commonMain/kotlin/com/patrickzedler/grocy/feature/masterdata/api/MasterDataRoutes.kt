package com.patrickzedler.grocy.feature.masterdata.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Overview of the master data categories, like Grocy's "Manage master data" menu; a detail of
 * the start screen, beside it where the window is wide enough.
 */
@Serializable
@SerialName("masterdata")
data object MasterDataRoute : DetailRoute

// Every list opens its objects as details, beside it where the window is wide enough; details
// without id create a new object

@Serializable
@SerialName("masterdata/products")
data object ProductsRoute : Route

/** Edits the product with [productId], or creates a new product without one. */
@Serializable
@SerialName("masterdata/product")
data class ProductRoute(val productId: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/locations")
data object LocationsRoute : Route

@Serializable
@SerialName("masterdata/location")
data class LocationRoute(val id: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/stores")
data object StoresRoute : Route

@Serializable
@SerialName("masterdata/store")
data class StoreRoute(val id: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/quantityunits")
data object QuantityUnitsRoute : Route

@Serializable
@SerialName("masterdata/quantityunit")
data class QuantityUnitRoute(val id: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/productgroups")
data object ProductGroupsRoute : Route

@Serializable
@SerialName("masterdata/productgroup")
data class ProductGroupRoute(val id: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/chores")
data object ChoresRoute : Route

@Serializable
@SerialName("masterdata/chore")
data class ChoreRoute(val id: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/batteries")
data object BatteriesRoute : Route

@Serializable
@SerialName("masterdata/battery")
data class BatteryRoute(val id: Int? = null) : DetailRoute

@Serializable
@SerialName("masterdata/taskcategories")
data object TaskCategoriesRoute : Route

@Serializable
@SerialName("masterdata/taskcategory")
data class TaskCategoryRoute(val id: Int? = null) : DetailRoute

/** The userfields, only those of [entity] (e.g. "products") if given, like Grocy's filter. */
@Serializable
@SerialName("masterdata/userfields")
data class UserfieldsRoute(val entity: String? = null) : Route

/** Edits a userfield, or creates one for [entity] without [id]. */
@Serializable
@SerialName("masterdata/userfield")
data class UserfieldRoute(val id: Int? = null, val entity: String? = null) : DetailRoute

@Serializable
@SerialName("masterdata/userentities")
data object UserentitiesRoute : Route

@Serializable
@SerialName("masterdata/userentity")
data class UserentityRoute(val id: Int? = null) : DetailRoute

// Grocy's stock pages its master data leads to

/**
 * Grocy's "Stock overview": every product in stock or below its min. stock amount; a detail of
 * the start screen, beside it where the window is wide enough.
 */
@Serializable
@SerialName("stockoverview")
data object StockOverviewRoute : DetailRoute

/** Grocy's "Stock entries", only those of [productId] if given. */
@Serializable
@SerialName("stockentries")
data class StockEntriesRoute(val productId: Int? = null) : Route

/** Grocy's "Stock journal", only the bookings of [productId] if given. */
@Serializable
@SerialName("stockjournal")
data class StockJournalRoute(val productId: Int? = null) : Route

/** Grocy's "Stock journal summary", only of [productId] if given. */
@Serializable
@SerialName("stockjournal/summary")
data class StockJournalSummaryRoute(val productId: Int? = null) : Route

// Grocy's stock bookings

/**
 * Grocy's "Purchase", the product [productId] preselected if given. From a shopping list item
 * ([shoppingListItemId]) with its [amount] (stock unit) in the unit [quId]; the item is removed
 * once the purchase is booked, like Grocy's "Add this item to stock".
 */
@Serializable
@SerialName("purchase")
data class PurchaseRoute(
    val productId: Int? = null,
    val amount: Double? = null,
    val quId: Int? = null,
    val shoppingListItemId: Int? = null,
) : Route

/** Grocy's "Consume"; [locationId] and [stockId] preselect a location and a stock entry. */
@Serializable
@SerialName("consume")
data class ConsumeRoute(
    val productId: Int? = null,
    val locationId: Int? = null,
    val stockId: String? = null,
) : Route

/** Grocy's "Transfer"; [locationId] and [stockId] preselect the source and a stock entry. */
@Serializable
@SerialName("transfer")
data class TransferRoute(
    val productId: Int? = null,
    val locationId: Int? = null,
    val stockId: String? = null,
) : Route

/** Grocy's "Inventory", the product [productId] preselected if given. */
@Serializable
@SerialName("inventory")
data class InventoryRoute(val productId: Int? = null) : Route

// Grocy's shopping list

/** Grocy's "Shopping list", the list [listId] (the first one without). */
@Serializable
@SerialName("shoppinglist")
data class ShoppingListRoute(val listId: Int? = null) : Route

/**
 * Grocy's shopping list item form: edits item [id], or creates an item on list [listId] (the
 * first one without) with product [productId] preselected if given; beside its list where the
 * window is wide enough, on its own e.g. as quick add from the start screen.
 */
@Serializable
@SerialName("shoppinglist/item")
data class ShoppingListItemRoute(
    val id: Int? = null,
    val listId: Int? = null,
    val productId: Int? = null,
) : DetailRoute

/** Grocy's shopping list form: renames list [id], or creates a list without. */
@Serializable
@SerialName("shoppinglist/list")
data class ShoppingListFormRoute(val id: Int? = null) : DetailRoute

val masterDataRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(MasterDataRoute::class, MasterDataRoute.serializer())
        subclass(ProductsRoute::class, ProductsRoute.serializer())
        subclass(ProductRoute::class, ProductRoute.serializer())
        subclass(LocationsRoute::class, LocationsRoute.serializer())
        subclass(LocationRoute::class, LocationRoute.serializer())
        subclass(StoresRoute::class, StoresRoute.serializer())
        subclass(StoreRoute::class, StoreRoute.serializer())
        subclass(QuantityUnitsRoute::class, QuantityUnitsRoute.serializer())
        subclass(QuantityUnitRoute::class, QuantityUnitRoute.serializer())
        subclass(ProductGroupsRoute::class, ProductGroupsRoute.serializer())
        subclass(ProductGroupRoute::class, ProductGroupRoute.serializer())
        subclass(ChoresRoute::class, ChoresRoute.serializer())
        subclass(ChoreRoute::class, ChoreRoute.serializer())
        subclass(BatteriesRoute::class, BatteriesRoute.serializer())
        subclass(BatteryRoute::class, BatteryRoute.serializer())
        subclass(TaskCategoriesRoute::class, TaskCategoriesRoute.serializer())
        subclass(TaskCategoryRoute::class, TaskCategoryRoute.serializer())
        subclass(UserfieldsRoute::class, UserfieldsRoute.serializer())
        subclass(UserfieldRoute::class, UserfieldRoute.serializer())
        subclass(UserentitiesRoute::class, UserentitiesRoute.serializer())
        subclass(UserentityRoute::class, UserentityRoute.serializer())
        subclass(PurchaseRoute::class, PurchaseRoute.serializer())
        subclass(ConsumeRoute::class, ConsumeRoute.serializer())
        subclass(TransferRoute::class, TransferRoute.serializer())
        subclass(InventoryRoute::class, InventoryRoute.serializer())
        subclass(ShoppingListRoute::class, ShoppingListRoute.serializer())
        subclass(ShoppingListItemRoute::class, ShoppingListItemRoute.serializer())
        subclass(ShoppingListFormRoute::class, ShoppingListFormRoute.serializer())
        subclass(StockOverviewRoute::class, StockOverviewRoute.serializer())
        subclass(StockEntriesRoute::class, StockEntriesRoute.serializer())
        subclass(StockJournalRoute::class, StockJournalRoute.serializer())
        subclass(StockJournalSummaryRoute::class, StockJournalSummaryRoute.serializer())
    }
}
