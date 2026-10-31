package com.patrickzedler.grocy.feature.masterdata.impl.conversion

import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversionFormTest {

    private val piece = QuantityUnit(1, "Piece", "Pieces")
    private val pack = QuantityUnit(2, "Pack", "Packs")
    private val units = listOf(piece, pack)

    @Test
    fun unitsAreRequiredAndCannotBeEqual() {
        assertEquals(
            mapOf(
                ConversionField.FROM to ConversionError.Required,
                ConversionField.TO to ConversionError.Required,
            ),
            ConversionFormValues().validate(decimals = 2),
        )
        assertEquals(
            mapOf(ConversionField.TO to ConversionError.SameUnit),
            ConversionFormValues(1, 1, "2").validate(decimals = 2),
        )
    }

    @Test
    fun factorMustBePositive() {
        val errors = ConversionFormValues(2, 1, "0").validate(decimals = 1)

        assertEquals(
            ConversionError.Number(FieldError.InvalidNumber("0.1", 1)),
            errors[ConversionField.FACTOR],
        )
    }

    @Test
    fun conversionKeepsItsProduct() {
        val conversion = ConversionFormValues(2, 1, "6,5").toConversion(id = 3, productId = 7)

        assertEquals(QuantityUnitConversion(3, 2, 1, 6.5, productId = 7), conversion)
    }

    @Test
    fun meaningIsExplainedBothWaysLikeGrocy() {
        val meaning = conversionMeaning(pack, piece, 6.0, 4, '.', "en")
        val inverse = conversionMeaning(pack, piece, 6.0, 4, '.', "en", inverse = true)

        assertEquals(ConversionMeaning("Pack", "6", "Pieces"), meaning)
        assertEquals(ConversionMeaning("Piece", "0.1667", "Packs"), inverse)
    }

    @Test
    fun productShowsItsOwnOverridesOnly() {
        val conversions = listOf(
            QuantityUnitConversion(1, 2, 1, 6.0),
            QuantityUnitConversion(2, 2, 1, 10.0, productId = 5),
        )

        val items = buildConversionItems(
            ConversionOwner.Product(5), conversions, units, 2, '.', "en",
        )

        assertEquals(listOf(ConversionItem(2, ConversionMeaning("Pack", "10", "Pieces"))), items)
    }

    @Test
    fun unitShowsTheDefaultConversionsFromIt() {
        val conversions = listOf(
            QuantityUnitConversion(1, 2, 1, 6.0),
            QuantityUnitConversion(2, 1, 2, 0.5),
            QuantityUnitConversion(3, 2, 1, 10.0, productId = 5),
        )

        val items = buildConversionItems(ConversionOwner.Unit(2), conversions, units, 2, '.', "en")

        assertEquals(listOf(1), items.map { it.id })
    }

    @Test
    fun resolvedConversionsAreGroupedByTheirUnitWithoutSelfConversions() {
        val resolved = listOf(
            ResolvedQuantityUnitConversion(5, 1, 1, 1.0),
            ResolvedQuantityUnitConversion(5, 2, 1, 6.0),
            ResolvedQuantityUnitConversion(5, 1, 2, 1.0 / 6),
            ResolvedQuantityUnitConversion(null, 2, 1, 6.0),
        )

        val groups = buildResolvedGroups(5, resolved, units, 2, '.', "en")

        assertEquals(listOf("Pack", "Piece"), groups.map { it.from })
        assertTrue(groups.all { it.conversions.size == 1 })
    }
}
