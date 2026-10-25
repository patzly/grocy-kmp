package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import com.patrickzedler.grocy.feature.masterdata.impl.related.numberError

/** The inputs of Grocy's shopping list item form for a given product. */
internal data class ShoppingListItemValues(
    val listId: Int?,
    val amount: String,
    val quId: Int?,
    val note: String,
)

internal enum class ShoppingListItemField { LIST, AMOUNT }

/** Grocy's rules: a list, and an amount of at least zero. */
internal fun ShoppingListItemValues.validate(
    decimals: Int,
): Map<ShoppingListItemField, FieldError> = buildMap {
    if (listId == null) put(ShoppingListItemField.LIST, FieldError.Required)
    numberError(amount, 0.0, decimals, required = true)
        ?.let { put(ShoppingListItemField.AMOUNT, it) }
}

/**
 * The amount in the product's stock unit, which Grocy stores: the typed amount in [unit]
 * divided by its factor; the amount itself without unit.
 */
internal fun ShoppingListItemValues.stockAmount(unit: AmountUnit?): Double {
    val typed = parseDecimal(amount) ?: 0.0
    return if (unit == null) typed else typed / unit.factor
}
