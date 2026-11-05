package com.patrickzedler.grocy.core.design

import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SeededPrimaryColorTest {

    @Test
    fun theHueOfTheAppearanceDecides() {
        assertNotEquals(
            seededPrimaryColor(AppearanceSettings(hue = 20f), isDark = false),
            seededPrimaryColor(AppearanceSettings(hue = 200f), isDark = false),
        )
    }

    @Test
    fun darkAndLightHaveTheirOwnPrimaryColor() {
        assertNotEquals(
            seededPrimaryColor(AppearanceSettings(), isDark = false),
            seededPrimaryColor(AppearanceSettings(), isDark = true),
        )
    }

    @Test
    fun theSameAppearanceAlwaysGivesTheSameColor() {
        assertEquals(
            seededPrimaryColor(AppearanceSettings(), isDark = true),
            seededPrimaryColor(AppearanceSettings(), isDark = true),
        )
    }
}
