package com.patrickzedler.grocy.core.data.productfacts

import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.productfacts.ProductFacts
import com.patrickzedler.grocy.core.network.ProductFactsApi
import com.patrickzedler.grocy.core.network.ProductFactsException

/** What the product databases said about a barcode. */
sealed interface ProductFactsResult {
    data class Found(val facts: ProductFacts) : ProductFactsResult

    data object NotFound : ProductFactsResult

    /** Nothing found, and at least one database could not be asked. */
    data object Failed : ProductFactsResult
}

/**
 * Asks the open product databases the user turned on about a barcode, one after the other until
 * one knows it. [enabledSources] are empty by default: nothing leaves the device before that.
 */
class ProductFactsRepository(
    private val api: ProductFactsApi,
    private val enabledSources: () -> Set<ProductFactsSource>,
) {

    val isEnabled: Boolean
        get() = enabledSources().isNotEmpty()

    /** What the first database that knows [barcode] says, in [language] where it can. */
    suspend fun lookUp(barcode: String, language: String): ProductFactsResult {
        val code = barcode.trim()
        if (!isLookupBarcode(code)) return ProductFactsResult.NotFound
        var failed = false
        for (source in ProductFactsSource.entries.filter { it in enabledSources() }) {
            try {
                val facts = api.product(source, code, language)
                if (facts != null) return ProductFactsResult.Found(facts)
            } catch (_: ProductFactsException) {
                failed = true
            }
        }
        return if (failed) ProductFactsResult.Failed else ProductFactsResult.NotFound
    }

    /** The product's picture, null when there is none to get. */
    suspend fun picture(facts: ProductFacts): ByteArray? = api.picture(facts)
}

/** The databases know retail barcodes (EAN, UPC, GTIN), other codes never leave the device. */
fun isLookupBarcode(code: String): Boolean =
    code.length in MIN_BARCODE_DIGITS..MAX_BARCODE_DIGITS && code.all { it in '0'..'9' }

private const val MIN_BARCODE_DIGITS = 8
private const val MAX_BARCODE_DIGITS = 14
