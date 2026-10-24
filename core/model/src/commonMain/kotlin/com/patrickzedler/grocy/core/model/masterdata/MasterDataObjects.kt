package com.patrickzedler.grocy.core.model.masterdata

data class ProductGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    /** Userfield values by field name. */
    val userfields: Map<String, String?> = emptyMap(),
)

data class Location(
    val id: Int,
    val name: String,
    val description: String? = null,
    val isFreezer: Boolean = false,
    val active: Boolean = true,
    val userfields: Map<String, String?> = emptyMap(),
)

data class QuantityUnit(
    val id: Int,
    val name: String,
    val namePlural: String? = null,
    val description: String? = null,
    val active: Boolean = true,
    /** Languages with more than two plural forms: one form per line. */
    val pluralForms: String? = null,
    val userfields: Map<String, String?> = emptyMap(),
)

/** A store, Grocy's `shopping_locations`. */
data class Store(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val userfields: Map<String, String?> = emptyMap(),
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

data class Battery(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val usedIn: String? = null,
    /** 0 turns the suggestion of the next charge cycle off. */
    val chargeIntervalDays: Int = 0,
    val userfields: Map<String, String?> = emptyMap(),
)

data class TaskCategory(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val userfields: Map<String, String?> = emptyMap(),
)

/** An entity the user defined, whose fields are userfields of `userentity-<name>`. */
data class Userentity(
    val id: Int,
    val name: String,
    val caption: String,
    val description: String? = null,
    val showInSidebarMenu: Boolean = false,
    val iconCssClass: String? = null,
)
