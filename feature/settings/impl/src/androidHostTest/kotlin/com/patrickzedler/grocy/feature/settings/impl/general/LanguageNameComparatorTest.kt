package com.patrickzedler.grocy.feature.settings.impl.general

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageNameComparatorTest {

    private val defaultLocale = Locale.getDefault()

    @AfterTest
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun accentedNamesSortByTheirLetterNotAtTheEnd() {
        Locale.setDefault(Locale.ENGLISH)

        val sorted = listOf("Deutsch", "Čeština", "English", "Català")
            .sortedWith(languageNameComparator())

        assertEquals(listOf("Català", "Čeština", "Deutsch", "English"), sorted)
    }
}
