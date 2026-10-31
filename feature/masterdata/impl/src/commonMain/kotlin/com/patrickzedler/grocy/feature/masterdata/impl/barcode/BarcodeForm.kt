package com.patrickzedler.grocy.feature.masterdata.impl.barcode

import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal

/** The inputs of Grocy's barcode form. */
internal data class BarcodeFormValues(
    val barcode: String = "",
    val amount: String = "",
    val quId: Int? = null,
    val storeId: Int? = null,
    val note: String = "",
)

internal enum class BarcodeField { BARCODE, AMOUNT }

/** Grocy's rules: the barcode is required, the amount optional but positive. */
internal fun BarcodeFormValues.validate(decimals: Int): Map<BarcodeField, FieldError> = buildMap {
    if (barcode.isBlank()) put(BarcodeField.BARCODE, FieldError.Required)
    numberError(amount, minimumAmount(decimals), decimals, required = false)
        ?.let { put(BarcodeField.AMOUNT, it) }
}

/** The barcode [id] of product [productId] to save from valid values. */
internal fun BarcodeFormValues.toBarcode(
    id: Int,
    productId: Int,
    original: ProductBarcode?,
    showsStore: Boolean,
) = ProductBarcode(
    id = id,
    productId = productId,
    barcode = barcode.trim(),
    quId = quId,
    amount = parseDecimal(amount),
    // Without price tracking the field is hidden and the stored store stays
    storeId = if (showsStore) storeId else original?.storeId,
    lastPrice = original?.lastPrice,
    note = note.trim().ifEmpty { null },
)

internal fun ProductBarcode.toFormValues(decimals: Int, decimalSeparator: Char) =
    BarcodeFormValues(
        barcode = barcode,
        amount = amount?.let { formatDecimal(it, decimals, decimalSeparator) }.orEmpty(),
        quId = quId,
        storeId = storeId,
        note = note.orEmpty(),
    )
