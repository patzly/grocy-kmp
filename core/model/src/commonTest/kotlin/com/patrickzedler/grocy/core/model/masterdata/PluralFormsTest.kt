package com.patrickzedler.grocy.core.model.masterdata

import kotlin.test.Test
import kotlin.test.assertEquals

class PluralFormsTest {

    @Test
    fun languagesHaveTheCountOfGrocysTranslation() {
        assertEquals(2, grocyPluralFormCount("en"))
        assertEquals(2, grocyPluralFormCount("de"))
        assertEquals(3, grocyPluralFormCount("fr"))
        assertEquals(4, grocyPluralFormCount("ru"))
        assertEquals(1, grocyPluralFormCount("ja"))
    }

    @Test
    fun regionalTagsUseTheirLanguage() {
        assertEquals(3, grocyPluralFormCount("pt-BR"))
        assertEquals(4, grocyPluralFormCount("cs_CZ"))
        assertEquals(2, grocyPluralFormCount("de-CH"))
    }

    @Test
    fun twoFormLanguagesUseThePluralForEverythingButOne() {
        assertEquals(0, grocyPluralIndex("de", 1.0))
        assertEquals(1, grocyPluralIndex("de", 0.0))
        assertEquals(1, grocyPluralIndex("en", 1.5))
        // Negative amounts of the journal are named like positive ones
        assertEquals(0, grocyPluralIndex("en", -1.0))
    }

    @Test
    fun rulesFollowGrocysTranslations() {
        assertEquals(0, grocyPluralIndex("fr", 0.0))
        assertEquals(2, grocyPluralIndex("fr", 2.0))
        assertEquals(0, grocyPluralIndex("ru", 21.0))
        assertEquals(1, grocyPluralIndex("ru", 3.0))
        assertEquals(2, grocyPluralIndex("ru", 11.0))
        assertEquals(1, grocyPluralIndex("pl", 22.0))
        assertEquals(2, grocyPluralIndex("pl", 12.0))
        assertEquals(2, grocyPluralIndex("cs", 1.5))
        assertEquals(3, grocyPluralIndex("cs", 5.0))
        assertEquals(0, grocyPluralIndex("tr", 1.0))
        assertEquals(0, grocyPluralIndex("ja", 7.0))
    }

    @Test
    fun unitIsNamedByTheFormOfTheAmount() {
        val unit = QuantityUnit(1, "Piece", "Pieces")

        assertEquals("Piece", unit.nameFor(1.0, "en"))
        assertEquals("Pieces", unit.nameFor(3.0, "en"))
    }

    @Test
    fun pluralFormsNameTheFormsAfterTheSingular() {
        val unit = QuantityUnit(1, "штука", "штуки", pluralForms = "штуки\nштук\nштуки")

        assertEquals("штука", unit.nameFor(1.0, "ru"))
        assertEquals("штуки", unit.nameFor(3.0, "ru"))
        assertEquals("штук", unit.nameFor(5.0, "ru"))
    }

    @Test
    fun missingPluralFormFallsBackToThePluralName() {
        val unit = QuantityUnit(1, "pièce", "pièces", pluralForms = "")

        assertEquals("pièces", unit.nameFor(2.0, "fr"))
    }
}
