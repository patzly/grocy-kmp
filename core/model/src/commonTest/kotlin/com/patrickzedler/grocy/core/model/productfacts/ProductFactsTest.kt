package com.patrickzedler.grocy.core.model.productfacts

import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductFactsTest {

    private val spread = ProductFacts(
        source = ProductFactsSource.FOOD,
        barcode = "3017620422003",
        packageAmount = 400.0,
        packageUnit = "g",
        energyKcalPer100 = 539.0,
    )

    private fun unit(name: String) = QuantityUnit(id = 1, name = name)

    @Test
    fun weightAndVolumeUnitsCountByTheirSize() {
        assertEquals(5.39, spread.energyPerStockUnit(unit("Gram")))
        assertEquals(5390.0, spread.energyPerStockUnit(unit("kg")))
        assertEquals(5390.0, spread.energyPerStockUnit(unit("Liter")))
    }

    @Test
    fun otherUnitsAndNoUnitCountOnePackage() {
        assertEquals(2156.0, spread.energyPerStockUnit(unit("Pack")))
        assertEquals(2156.0, spread.energyPerStockUnit(null))
    }

    @Test
    fun withoutThePackagesSizeOnlyWeightUnitsAreKnown() {
        val unknownSize = spread.copy(packageAmount = null, packageUnit = null)

        assertNull(unknownSize.energyPerStockUnit(unit("Pack")))
        assertEquals(5.39, unknownSize.energyPerStockUnit(unit("g")))
    }

    @Test
    fun noEnergyStaysUnknown() {
        assertNull(spread.copy(energyKcalPer100 = null).energyPerStockUnit(unit("g")))
    }
}
