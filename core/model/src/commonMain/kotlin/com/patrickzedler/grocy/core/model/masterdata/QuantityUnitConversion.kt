package com.patrickzedler.grocy.core.model.masterdata

/**
 * A conversion between two quantity units, Grocy's `quantity_unit_conversions`: 1 of
 * [fromQuId] is [factor] of [toQuId]. Without [productId] a default conversion of every
 * product, with one an override for that product.
 */
data class QuantityUnitConversion(
    val id: Int,
    val fromQuId: Int,
    val toQuId: Int,
    val factor: Double,
    val productId: Int? = null,
    val userfields: Map<String, String?> = emptyMap(),
)

/**
 * A conversion Grocy derived from the defined ones (`quantity_unit_conversions_resolved`),
 * directly or through other units, per product or for every product without [productId].
 */
data class ResolvedQuantityUnitConversion(
    val productId: Int?,
    val fromQuId: Int,
    val toQuId: Int,
    val factor: Double,
)
