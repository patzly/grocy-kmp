package com.patrickzedler.grocy.core.model

import com.patrickzedler.grocy.core.model.text.DateFieldOrder

/** The order of typed dates, [AUTOMATIC] follows how the app's language writes dates. */
enum class DateInputOrder(val fixed: DateFieldOrder?) {
    AUTOMATIC(null),
    DAY_FIRST(DateFieldOrder.DAY_MONTH_YEAR),
    MONTH_FIRST(DateFieldOrder.MONTH_DAY_YEAR),
    YEAR_FIRST(DateFieldOrder.YEAR_MONTH_DAY),
}

/** The open product databases that can fill in a new product by its barcode. */
enum class ProductFactsSource(val title: String, val domain: String) {
    FOOD("Open Food Facts", "openfoodfacts.org"),
    BEAUTY("Open Beauty Facts", "openbeautyfacts.org"),
    PET_FOOD("Open Pet Food Facts", "openpetfoodfacts.org"),
    PRODUCTS("Open Products Facts", "openproductsfacts.org"),
}

/**
 * How the app behaves, kept on the device: the databases asked about unknown barcodes (none
 * until chosen, a barcode only leaves for them with the user's consent), typed dates instead
 * of the date picker and whether a barcode created by a purchase keeps its amount.
 */
data class BehaviorSettings(
    val productFactsSources: Set<ProductFactsSource> = emptySet(),
    val dateKeyboard: Boolean = false,
    val dateInputOrder: DateInputOrder = DateInputOrder.AUTOMATIC,
    val barcodeUsesAmount: Boolean = true,
) {
    companion object {
        /** Settings from their stored parts; missing or unknown values fall back to defaults. */
        fun of(
            productFactsSources: String?,
            dateKeyboard: Boolean?,
            dateInputOrder: String?,
            barcodeUsesAmount: Boolean?,
        ): BehaviorSettings {
            val default = BehaviorSettings()
            return BehaviorSettings(
                productFactsSources = productFactsSources.orEmpty().split(',')
                    .mapNotNull { name -> ProductFactsSource.entries.find { it.name == name } }
                    .toSet(),
                dateKeyboard = dateKeyboard ?: default.dateKeyboard,
                dateInputOrder = DateInputOrder.entries.find { it.name == dateInputOrder }
                    ?: default.dateInputOrder,
                barcodeUsesAmount = barcodeUsesAmount ?: default.barcodeUsesAmount,
            )
        }

        /** The sources as they are stored, enum names joined with commas. */
        fun storedSources(sources: Set<ProductFactsSource>): String =
            sources.joinToString(",") { it.name }
    }
}
