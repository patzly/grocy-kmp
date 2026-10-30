package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectField
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import com.patrickzedler.grocy.feature.masterdata.impl.related.numberError
import com.patrickzedler.grocy.feature.masterdata.impl.related.productAmountUnits

/** The inputs of Grocy's shopping list item form. */
internal data class ShoppingListItemValues(
    val listId: Int?,
    val productId: Int?,
    val amount: String,
    val quId: Int?,
    val note: String,
)

/**
 * Grocy's rules: a list, an amount of at least zero with the allowed decimal places, and a
 * product or a note (one message on both).
 */
internal fun ShoppingListItemValues.validate(decimals: Int): Map<ObjectField, FieldError> =
    buildMap {
        if (listId == null) put(ObjectField.SHOPPING_LIST, FieldError.Required)
        numberError(amount, 0.0, decimals, required = true)
            ?.let { put(ObjectField.AMOUNT, it) }
        if (productId == null && note.isBlank()) {
            put(ObjectField.PRODUCT, FieldError.Required)
            put(ObjectField.NOTE, FieldError.Required)
        }
    }

/**
 * The amount in the product's stock unit, which Grocy stores: the typed amount in [unit]
 * divided by its factor; the amount itself without unit (items without product).
 */
internal fun ShoppingListItemValues.stockAmount(unit: AmountUnit?): Double {
    val typed = parseDecimal(amount) ?: 0.0
    return if (unit == null) typed else typed / unit.factor
}

/**
 * The item as Grocy's form saves it: the amount in the stock unit with the chosen unit for
 * products, the plain number without; [done] stays as it is.
 */
internal fun ShoppingListItemValues.toItem(
    id: Int,
    done: Boolean,
    unit: AmountUnit?,
    userfields: Map<String, String?> = emptyMap(),
): ShoppingListItem = ShoppingListItem(
    id = id,
    listId = listId ?: ShoppingList.DEFAULT_ID,
    productId = productId,
    note = note.trim().ifEmpty { null },
    amount = stockAmount(unit.takeIf { productId != null }),
    quId = quId.takeIf { productId != null },
    done = done,
    userfields = userfields,
)

/** The units an item of [product] can be shown in, converted from its stock unit. */
internal fun itemUnits(
    product: Product?,
    resolved: List<ResolvedQuantityUnitConversion>,
    units: List<QuantityUnit>,
): List<AmountUnit> = product?.let {
    productAmountUnits(it.id, it.quIdStock, resolved, units)
}.orEmpty()

/**
 * The unit a newly chosen product starts with, like Grocy: its purchase unit if it converts
 * into it, otherwise its stock unit.
 */
internal fun defaultItemQuId(product: Product?, units: List<AmountUnit>): Int? =
    units.firstOrNull { it.unit.id == product?.quIdPurchase }?.unit?.id
        ?: product?.quIdStock

/** The amount of a stored item as the form shows it: in its unit, or the plain number. */
internal fun itemAmountText(
    item: ShoppingListItem,
    units: List<AmountUnit>,
    decimals: Int,
    decimalSeparator: Char,
): String {
    val factor = units.firstOrNull { it.unit.id == item.quId }?.factor ?: 1.0
    return formatDecimal(item.amount * factor, decimals, decimalSeparator)
}

/** The product a scanned [code] stands for: a grocycode ("grcy:p:12") or a known barcode. */
internal fun productIdForCode(code: String, barcodes: List<ProductBarcode>): Int? {
    val trimmed = code.trim()
    return GROCYCODE.matchEntire(trimmed)?.groupValues?.get(1)?.toIntOrNull()
        ?: barcodes.firstOrNull { it.barcode == trimmed }?.productId
}

private val GROCYCODE = Regex("""grcy:p:(\d+)(:.*)?""")
