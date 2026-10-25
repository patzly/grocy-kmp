package com.patrickzedler.grocy.feature.masterdata.impl.related

import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.text.decimalPlaces
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldInput
import com.patrickzedler.grocy.feature.masterdata.impl.product.defaultUserfieldValue
import com.patrickzedler.grocy.feature.masterdata.impl.product.validateUserfield

/**
 * Grocy's check of a number field: at least [min], at most [places] decimal places; an empty
 * field is fine unless [required]. Null when the text is fine.
 */
internal fun numberError(
    text: String,
    min: Double,
    places: Int,
    required: Boolean,
): FieldError? {
    if (text.isBlank()) return if (required) FieldError.Required else null
    val value = parseDecimal(text)
    return if (value == null || value < min || decimalPlaces(text) > places) {
        FieldError.InvalidNumber(formatDecimal(min, places), places)
    } else {
        null
    }
}

/** A unit an amount of a product can be entered in; [factor] of it are one destination unit. */
internal data class AmountUnit(val unit: QuantityUnit, val factor: Double)

/**
 * The units of Grocy's product amount picker: the ones [resolved] converts the product's
 * [destinationQuId] (e.g. its stock or purchase unit) into, the destination unit itself with
 * factor 1, one per unit, by name. Without known conversions only the destination unit.
 */
internal fun productAmountUnits(
    productId: Int,
    destinationQuId: Int?,
    resolved: List<ResolvedQuantityUnitConversion>,
    units: List<QuantityUnit>,
): List<AmountUnit> {
    val byId = units.associateBy { it.id }
    val destination = destinationQuId?.let { byId[it] } ?: return emptyList()
    val converted = resolved
        .filter { it.productId == productId && it.fromQuId == destinationQuId }
        .mapNotNull { conversion ->
            val factor = if (conversion.toQuId == destinationQuId) 1.0 else conversion.factor
            byId[conversion.toQuId]?.let { AmountUnit(it, factor) }
        }
    return (listOf(AmountUnit(destination, 1.0)) + converted)
        .distinctBy { it.unit.id }
        .sortedBy { it.unit.name.lowercase() }
}

/** Grocy's check of every userfield input, by field name. */
internal fun List<UserfieldInput>.validationErrors(): Map<String, UserfieldError> =
    mapNotNull { input ->
        validateUserfield(input.userfield, input.value)?.let { input.userfield.name to it }
    }.toMap()

/** The values of the touched inputs by field name, like Grocy sends only changed userfields. */
internal fun List<UserfieldInput>.changedValues(): Map<String, String?> =
    filter { it.isChanged }.associate { it.userfield.name to it.value }

/**
 * Inputs of the [fields] of an object: its stored [values], or the fields' defaults for a new
 * object without them.
 */
internal fun userfieldInputs(
    fields: List<Userfield>,
    values: Map<String, String?>?,
): List<UserfieldInput> = fields.map { field ->
    UserfieldInput(field, if (values != null) values[field.name] else defaultUserfieldValue(field))
}

/**
 * The amount typed in [unit] in the destination unit of the picker, for Grocy's "This equals"
 * below it; null without amount or in the destination unit itself.
 */
internal fun destinationAmount(amountText: String, unit: AmountUnit?): Double? {
    if (unit == null || unit.factor == 1.0) return null
    val amount = parseDecimal(amountText) ?: return null
    return amount / unit.factor
}
