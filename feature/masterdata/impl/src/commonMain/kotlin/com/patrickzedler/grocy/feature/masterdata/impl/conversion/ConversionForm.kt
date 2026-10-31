package com.patrickzedler.grocy.feature.masterdata.impl.conversion

import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal

/** The inputs of Grocy's conversion form. */
internal data class ConversionFormValues(
    val fromQuId: Int? = null,
    val toQuId: Int? = null,
    val factor: String = "1",
)

internal sealed interface ConversionError {
    data object Required : ConversionError

    /** Grocy's "This cannot be equal to %s": a unit cannot be converted into itself. */
    data object SameUnit : ConversionError

    data class Number(val error: FieldError) : ConversionError
}

internal enum class ConversionField { FROM, TO, FACTOR }

/** Grocy's rules: both units, different ones, and a positive factor. */
internal fun ConversionFormValues.validate(decimals: Int): Map<ConversionField, ConversionError> =
    buildMap {
        if (fromQuId == null) put(ConversionField.FROM, ConversionError.Required)
        when {
            toQuId == null -> put(ConversionField.TO, ConversionError.Required)
            toQuId == fromQuId -> put(ConversionField.TO, ConversionError.SameUnit)
        }
        numberError(factor, minimumAmount(decimals), decimals, required = true)
            ?.let { put(ConversionField.FACTOR, ConversionError.Number(it)) }
    }

/** The conversion [id] to save from valid values; [productId] null for a default one. */
internal fun ConversionFormValues.toConversion(id: Int, productId: Int?) = QuantityUnitConversion(
    id = id,
    fromQuId = requireNotNull(fromQuId),
    toQuId = requireNotNull(toQuId),
    factor = requireNotNull(parseDecimal(factor)),
    productId = productId,
)

internal fun QuantityUnitConversion.toFormValues(decimals: Int, decimalSeparator: Char) =
    ConversionFormValues(
        fromQuId = fromQuId,
        toQuId = toQuId,
        factor = formatDecimal(factor, decimals, decimalSeparator),
    )

/** One side of Grocy's "This means 1 %1$s is the same as %2$s %3$s". */
internal data class ConversionMeaning(val from: String, val amount: String, val to: String)

/**
 * What 1 of [from] is in [to] with [factor], for Grocy's explanation below the factor; with
 * [inverse] what 1 of [to] is in [from].
 */
internal fun conversionMeaning(
    from: QuantityUnit,
    to: QuantityUnit,
    factor: Double,
    decimals: Int,
    decimalSeparator: Char,
    language: String,
    inverse: Boolean = false,
): ConversionMeaning {
    val amount = if (inverse) 1 / factor else factor
    val (one, other) = if (inverse) to to from else from to to
    return ConversionMeaning(
        from = one.name,
        amount = formatDecimal(amount, decimals, decimalSeparator),
        to = other.nameFor(amount, language),
    )
}
