package com.patrickzedler.grocy.feature.masterdata.impl.related

import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormInputsTest {

    private val piece = QuantityUnit(1, "Piece", "Pieces")
    private val pack = QuantityUnit(2, "Pack", "Packs")
    private val box = QuantityUnit(3, "Box", "Boxes")

    @Test
    fun numberNeedsItsMinimumAndDecimalPlaces() {
        assertNull(numberError("1,5", 0.0, 2, required = true))
        assertEquals(FieldError.InvalidNumber("0.1", 1), numberError("0.05", 0.1, 1, true))
        assertEquals(FieldError.InvalidNumber("0", 0), numberError("-1", 0.0, 0, true))
    }

    @Test
    fun emptyNumberIsOnlyMissingWhenRequired() {
        assertEquals(FieldError.Required, numberError(" ", 0.0, 2, required = true))
        assertNull(numberError("", 0.0, 2, required = false))
    }

    @Test
    fun amountUnitsAreTheOnesTheProductConvertsInto() {
        val resolved = listOf(
            ResolvedQuantityUnitConversion(1, fromQuId = 1, toQuId = 1, factor = 1.0),
            ResolvedQuantityUnitConversion(1, fromQuId = 1, toQuId = 2, factor = 0.25),
            ResolvedQuantityUnitConversion(1, fromQuId = 2, toQuId = 1, factor = 4.0),
            // Another product's conversion and a default one do not count
            ResolvedQuantityUnitConversion(2, fromQuId = 1, toQuId = 3, factor = 0.1),
            ResolvedQuantityUnitConversion(null, fromQuId = 1, toQuId = 3, factor = 0.1),
        )

        val units = productAmountUnits(1, destinationQuId = 1, resolved, listOf(piece, pack, box))

        assertEquals(listOf(AmountUnit(pack, 0.25), AmountUnit(piece, 1.0)), units)
    }

    @Test
    fun withoutConversionsOnlyTheDestinationUnitIsOffered() {
        val units = productAmountUnits(1, destinationQuId = 2, emptyList(), listOf(piece, pack))

        assertEquals(listOf(AmountUnit(pack, 1.0)), units)
    }

    @Test
    fun amountInAnotherUnitIsConvertedIntoTheDestination() {
        assertEquals(8.0, destinationAmount("2", AmountUnit(pack, 0.25)))
        assertNull(destinationAmount("2", AmountUnit(piece, 1.0)))
        assertNull(destinationAmount("", AmountUnit(pack, 0.25)))
    }
}
