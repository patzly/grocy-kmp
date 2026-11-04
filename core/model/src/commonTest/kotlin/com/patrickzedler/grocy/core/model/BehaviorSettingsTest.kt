package com.patrickzedler.grocy.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BehaviorSettingsTest {

    @Test
    fun databasesAreOffAndTheAmountIsUsedByDefault() {
        val settings = BehaviorSettings()

        assertEquals(emptySet(), settings.productFactsSources)
        assertEquals(false, settings.dateKeyboard)
        assertEquals(DateInputOrder.AUTOMATIC, settings.dateInputOrder)
        assertEquals(true, settings.barcodeUsesAmount)
    }

    @Test
    fun nothingStoredGivesTheDefaults() {
        assertEquals(BehaviorSettings(), BehaviorSettings.of(null, null, null, null))
    }

    @Test
    fun storedValuesAreReadBack() {
        val stored = BehaviorSettings.storedSources(
            setOf(ProductFactsSource.FOOD, ProductFactsSource.BEAUTY),
        )

        val settings = BehaviorSettings.of(stored, true, "YEAR_FIRST", false)

        assertEquals(
            setOf(ProductFactsSource.FOOD, ProductFactsSource.BEAUTY),
            settings.productFactsSources,
        )
        assertEquals(true, settings.dateKeyboard)
        assertEquals(DateInputOrder.YEAR_FIRST, settings.dateInputOrder)
        assertEquals(false, settings.barcodeUsesAmount)
    }

    @Test
    fun unknownValuesOfANewerVersionAreIgnored() {
        val settings = BehaviorSettings.of("FOOD,MARS", null, "SIDEWAYS", null)

        assertEquals(setOf(ProductFactsSource.FOOD), settings.productFactsSources)
        assertEquals(DateInputOrder.AUTOMATIC, settings.dateInputOrder)
    }

    @Test
    fun theStoredFormatStaysReadable() {
        val settings = BehaviorSettings.of("PET_FOOD,PRODUCTS", null, "DAY_FIRST", null)

        assertEquals(
            setOf(ProductFactsSource.PET_FOOD, ProductFactsSource.PRODUCTS),
            settings.productFactsSources,
        )
        assertEquals(DateInputOrder.DAY_FIRST, settings.dateInputOrder)
    }
}
