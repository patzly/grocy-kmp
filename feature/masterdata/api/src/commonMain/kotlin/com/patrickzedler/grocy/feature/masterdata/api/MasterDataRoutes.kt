package com.patrickzedler.grocy.feature.masterdata.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Overview of the master data categories, like Grocy's "Manage master data" menu. */
@Serializable
@SerialName("masterdata")
data object MasterDataRoute : Route

@Serializable
@SerialName("masterdata/products")
data object ProductsRoute : Route

/** Edits the product with [productId], or creates a new product without one. */
@Serializable
@SerialName("masterdata/product")
data class ProductRoute(val productId: Int? = null) : Route

val masterDataRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(MasterDataRoute::class, MasterDataRoute.serializer())
        subclass(ProductsRoute::class, ProductsRoute.serializer())
        subclass(ProductRoute::class, ProductRoute.serializer())
    }
}
