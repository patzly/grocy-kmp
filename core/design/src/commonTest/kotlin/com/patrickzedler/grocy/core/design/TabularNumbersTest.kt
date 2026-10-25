package com.patrickzedler.grocy.core.design

import androidx.compose.ui.text.TextStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class TabularNumbersTest {

    @Test
    fun styleGetsTabularFigures() {
        assertEquals("tnum", TextStyle().withTabularNumbers().fontFeatureSettings)
    }

    @Test
    fun otherFeaturesStay() {
        val style = TextStyle(fontFeatureSettings = "smcp").withTabularNumbers()

        assertEquals("smcp, tnum", style.fontFeatureSettings)
    }

    @Test
    fun tabularFiguresAreNotAddedTwice() {
        val style = TextStyle(fontFeatureSettings = "smcp, tnum")

        assertEquals("smcp, tnum", style.withTabularNumbers().fontFeatureSettings)
    }
}
