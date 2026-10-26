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
        subclass(StockOverviewRoute::class, StockOverviewRoute.serializer())
        subclass(StockEntriesRoute::class, StockEntriesRoute.serializer())
        subclass(StockJournalRoute::class, StockJournalRoute.serializer())
        subclass(StockJournalSummaryRoute::class, StockJournalSummaryRoute.serializer())
    }
}
