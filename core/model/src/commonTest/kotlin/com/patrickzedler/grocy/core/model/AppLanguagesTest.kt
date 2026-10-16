package com.patrickzedler.grocy.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppLanguagesTest {

    private val languages = listOf("de", "pt-BR")

    @Test
    fun listedTagResolvesToItself() {
        assertEquals("pt-BR", resolveAppLanguage("pt-BR", languages))
    }

    @Test
    fun tagsCompareWithoutCase() {
        assertEquals("pt-BR", resolveAppLanguage("pt-br", languages))
    }

    @Test
    fun regionalTagFallsBackToItsLanguage() {
        assertEquals("de", resolveAppLanguage("de-CH", languages))
    }

    @Test
    fun systemOrUnknownLanguageResolvesToNothing() {
        assertNull(resolveAppLanguage(null, languages))
        assertNull(resolveAppLanguage("fr", languages))
    }

    @Test
    fun englishIsTheFirstLanguage() {
        // values without qualifier, the fallback for every other language
        assertEquals("en", AppLanguages.first())
    }

    @Test
    fun hebrewIsRightToLeftWithAnyRegionOrCode() {
        assertTrue(isRightToLeft("he"))
        assertTrue(isRightToLeft("he-IL"))
        assertTrue(isRightToLeft("iw"))
    }

    @Test
    fun otherAppLanguagesAreLeftToRight() {
        assertEquals(listOf("he"), AppLanguages.filter(::isRightToLeft))
    }
}
