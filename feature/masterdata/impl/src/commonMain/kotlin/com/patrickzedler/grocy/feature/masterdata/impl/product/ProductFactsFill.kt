package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.productfacts.ProductFacts
import com.patrickzedler.grocy.core.model.productfacts.ProductNutrient
import com.patrickzedler.grocy.core.model.productfacts.energyPerStockUnit
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.product_facts_carbohydrates
import com.patrickzedler.grocy.core.resources.product_facts_fat
import com.patrickzedler.grocy.core.resources.product_facts_fiber
import com.patrickzedler.grocy.core.resources.product_facts_per
import com.patrickzedler.grocy.core.resources.product_facts_protein
import com.patrickzedler.grocy.core.resources.product_facts_salt
import com.patrickzedler.grocy.core.resources.product_facts_saturated_fat
import com.patrickzedler.grocy.core.resources.product_facts_sugars
import org.jetbrains.compose.resources.getString

/** The words of the nutrition line of a filled in description, from the resources. */
internal class ProductFactsTexts(
    val nutrient: suspend (ProductNutrient) -> String = { it.name },
    val per: suspend (String) -> String = { it },
)

/** The texts from the resources, read when a lookup needs them. */
internal fun productFactsTexts() = ProductFactsTexts(
    nutrient = { nutrient ->
        getString(
            when (nutrient) {
                ProductNutrient.FAT -> Res.string.product_facts_fat
                ProductNutrient.SATURATED_FAT -> Res.string.product_facts_saturated_fat
                ProductNutrient.CARBOHYDRATES -> Res.string.product_facts_carbohydrates
                ProductNutrient.SUGARS -> Res.string.product_facts_sugars
                ProductNutrient.FIBER -> Res.string.product_facts_fiber
                ProductNutrient.PROTEIN -> Res.string.product_facts_protein
                ProductNutrient.SALT -> Res.string.product_facts_salt
            },
        )
    },
    per = { getString(Res.string.product_facts_per, it) },
)

/**
 * The product's description from what Grocy has no field for: its kind, brand and package,
 * ingredients and nutrients per 100 g or ml.
 */
internal suspend fun ProductFacts.description(
    texts: ProductFactsTexts,
    decimalSeparator: Char,
): String {
    val nutrition = ProductNutrient.entries.mapNotNull { nutrient ->
        nutrition[nutrient]?.let { amount ->
            val shown = formatDecimal(amount, NUTRIENT_DECIMALS, decimalSeparator)
            "${texts.nutrient(nutrient)} $shown g"
        }
    }
    val per = "100 ${if (packageUnit?.lowercase()?.endsWith("l") == true) "ml" else "g"}"
    return listOfNotNull(
        genericName?.takeIf { it != name },
        listOfNotNull(brands, quantity).joinToString(" · ").ifEmpty { null },
        ingredients,
        nutrition.takeIf { it.isNotEmpty() }
            ?.let { "${texts.per(per)}: ${it.joinToString(" · ")}" },
    ).joinToString("\n")
}

/**
 * Fills what is still empty in [form] from the database, nothing the user or their presets
 * entered is replaced. Product group and store only when one of the user's is named the same.
 *
 * @return the energy filled in, to follow a later change of the stock unit; null if none.
 */
internal suspend fun ProductFacts.fillInto(
    form: ProductFormState,
    options: ProductOptions,
    texts: ProductFactsTexts,
    decimals: Int,
    decimalSeparator: Char,
): String? {
    if (form.name.text.isBlank()) name?.let { form.name.setTextAndPlaceCursorAtEnd(it) }
    if (form.description.text.isBlank()) {
        form.description.setTextAndPlaceCursorAtEnd(description(texts, decimalSeparator))
    }
    if (form.productGroupId == null) {
        // The most specific category comes last
        form.productGroupId = categories.asReversed().firstNotNullOfOrNull { category ->
            options.productGroups.firstOrNull { it.name.equals(category, ignoreCase = true) }?.id
        }
    }
    if (form.storeId == null) {
        form.storeId = stores.firstNotNullOfOrNull { store ->
            options.stores.firstOrNull { it.name.equals(store, ignoreCase = true) }?.id
        }
    }
    if (form.calories.text.isNotBlank()) return null
    val energy = energyText(options.quantityUnits, form.quIdStock, decimals, decimalSeparator)
        ?: return null
    form.calories.setTextAndPlaceCursorAtEnd(energy)
    return energy
}

/** The energy for the stock unit [quId] as the form shows numbers, null if it is not known. */
internal fun ProductFacts.energyText(
    units: List<QuantityUnit>,
    quId: Int?,
    decimals: Int,
    decimalSeparator: Char,
): String? = energyPerStockUnit(units.firstOrNull { it.id == quId })
    ?.let { formatDecimal(it, decimals, decimalSeparator) }

private const val NUTRIENT_DECIMALS = 1
