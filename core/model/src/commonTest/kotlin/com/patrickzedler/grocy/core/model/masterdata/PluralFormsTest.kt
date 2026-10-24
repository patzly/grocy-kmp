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
}
