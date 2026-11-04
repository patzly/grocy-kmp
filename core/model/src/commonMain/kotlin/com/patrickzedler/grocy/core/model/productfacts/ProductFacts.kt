package com.patrickzedler.grocy.core.model.productfacts

import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit

/** The nutrients of a product besides its energy, per 100 g or ml. */
enum class ProductNutrient {
    FAT,
    SATURATED_FAT,
    CARBOHYDRATES,
    SUGARS,
    FIBER,
    PROTEIN,
    SALT,
}

/** What an open product database knows about a barcode. */
data class ProductFacts(
    val source: ProductFactsSource,
    val barcode: String,
    val name: String? = null,
    val genericName: String? = null,
    val brands: String? = null,
    /** The package as printed, e.g. "400 g". */
    val quantity: String? = null,
    /** The content of a package in [packageUnit]. */
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val energyKcalPer100: Double? = null,
    val nutrition: Map<ProductNutrient, Double> = emptyMap(),
    val ingredients: String? = null,
    val categories: List<String> = emptyList(),
    val stores: List<String> = emptyList(),
    val imageUrl: String? = null,
)

/**
 * The energy per [stockUnit], which Grocy keeps per stock unit: a unit of weight or volume by its
 * name, anything else counts as one package. Null where the package's size would be needed but
 * is not known.
 */
fun ProductFacts.energyPerStockUnit(stockUnit: QuantityUnit?): Double? {
    val per100 = energyKcalPer100 ?: return null
    val base = stockUnit?.let { unit ->
        listOfNotNull(unit.name, unit.namePlural)
            .firstNotNullOfOrNull { unitBaseAmounts[it.lowercase().trim()] }
    }
    if (base != null) return per100 * base / 100
    val amount = packageAmount ?: return null
    val packageBase = unitBaseAmounts[packageUnit?.lowercase()?.trim()] ?: return null
    return per100 * amount * packageBase / 100
}

// Grams or milliliters of the units people name their stock in; energy is per 100 of either
private val unitBaseAmounts = mapOf(
    "g" to 1.0, "gr" to 1.0, "gram" to 1.0, "grams" to 1.0, "gramm" to 1.0,
    "kg" to 1000.0, "kilo" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0,
    "kilogramm" to 1000.0,
    "mg" to 0.001,
    "ml" to 1.0, "milliliter" to 1.0, "milliliters" to 1.0, "millilitre" to 1.0,
    "millilitres" to 1.0,
    "cl" to 10.0, "dl" to 100.0,
    "l" to 1000.0, "liter" to 1000.0, "liters" to 1000.0, "litre" to 1000.0, "litres" to 1000.0,
)
