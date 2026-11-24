package com.patrickzedler.grocy.feature.recipes.impl.form

import com.patrickzedler.grocy.core.forms.productAmountUnits
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.recipes.IngredientLine
import com.patrickzedler.grocy.core.model.recipes.matchUnit
import com.patrickzedler.grocy.core.model.recipes.parseIngredientLine
import com.patrickzedler.grocy.core.model.recipes.suggestProduct
import com.patrickzedler.grocy.core.model.recipes.unitWordsOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.network.RecipeImportFailure

/** Where reading the recipe site's page stands. */
internal sealed interface RecipeImportState {
    data object Loading : RecipeImportState

    data class Failed(val failure: RecipeImportFailure) : RecipeImportState

    data object Done : RecipeImportState
}

/**
 * An ingredient line of the imported recipe that is no Grocy ingredient yet, with what the app
 * proposes for it: a product, the amount and a unit the product's amount converts into.
 */
internal data class ImportLine(
    /** Its place among the site's lines, stays when others are added or removed. */
    val key: Int,
    val line: IngredientLine,
    val productId: Int?,
    val productName: String?,
    val amount: String,
    val quId: Int?,
    val unitName: String?,
    /**
     * Added without asking by "Add all proposed": a confident product, an amount and a unit the
     * product's stock amount converts into.
     */
    val isReady: Boolean,
)

/**
 * The [lines] of a recipe site as proposals: each taken apart ([parseIngredientLine], Grocy's
 * unit names count as units), the product most likely meant ([suggestProduct]), its unit found
 * among the units the product converts into; a line without unit takes the product's stock unit.
 */
internal fun importLinesOf(
    lines: List<String>,
    products: List<Product>,
    units: List<QuantityUnit>,
    conversions: List<ResolvedQuantityUnitConversion>,
    decimals: Int,
    decimalSeparator: Char,
): List<ImportLine> {
    val unitWords = unitWordsOf(units)
    return lines.mapIndexed { index, text ->
        val line = parseIngredientLine(text, unitWords)
        val suggestion = suggestProduct(line.name, products)
        val product = suggestion?.product
        val amountUnits = product?.let {
            productAmountUnits(it.id, it.quIdStock, conversions, units)
        }.orEmpty()
        val unit = when {
            product == null -> null
            line.unit == null -> amountUnits.firstOrNull { it.unit.id == product.quIdStock }?.unit
            else -> matchUnit(line.unit, amountUnits.map { it.unit })
        }
        ImportLine(
            key = index,
            line = line,
            productId = product?.id,
            productName = product?.name,
            amount = line.amount?.let { formatDecimal(it, decimals, decimalSeparator) }.orEmpty(),
            quId = unit?.id,
            unitName = unit?.name,
            isReady = suggestion?.isConfident == true && line.amount != null && unit != null,
        )
    }
}

/** The ingredient form an imported line opens with: its proposals and its note. */
internal fun ImportLine.toFormValues() = IngredientFormValues(
    productId = productId,
    amount = amount,
    quId = quId,
    note = line.note.orEmpty(),
)
