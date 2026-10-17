package com.patrickzedler.grocy.core.model.masterdata

data class ProductGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
)

data class Location(
    val id: Int,
    val name: String,
    val description: String? = null,
    val isFreezer: Boolean = false,
    val active: Boolean = true,
)

data class QuantityUnit(
    val id: Int,
    val name: String,
    val namePlural: String? = null,
    val description: String? = null,
    val active: Boolean = true,
)

/** A store, Grocy's `shopping_locations`. */
data class Store(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
)

data class ProductBarcode(
    val id: Int,
    val productId: Int,
    val barcode: String,
    val quId: Int? = null,
    val amount: Double? = null,
    val storeId: Int? = null,
    val lastPrice: Double? = null,
    val note: String? = null,
)

/** Current stock of a product, the part of Grocy's `/stock` the master data needs. */
data class StockAmount(
    val productId: Int,
    val amount: Double,
    /** Including the stock of sub products for parent products. */
    val amountAggregated: Double,
)
