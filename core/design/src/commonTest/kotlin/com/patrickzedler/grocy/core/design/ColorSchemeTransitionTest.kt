package com.patrickzedler.grocy.core.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.lerp
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSchemeTransitionTest {

    private val light = lightColorScheme()
    private val dark = darkColorScheme()

    @Test
    fun startOfTheTransitionIsTheOldScheme() {
        assertEquals(light.toString(), lerp(light, dark, 0f).toString())
    }

    @Test
    fun endOfTheTransitionIsTheNewScheme() {
        assertEquals(dark.toString(), lerp(light, dark, 1f).toString())
    }

    @Test
    fun everyColorIsBlended() {
        val half = lerp(light, dark, 0.5f)

        assertEquals(
            lerp(light.surfaceContainer, dark.surfaceContainer, 0.5f),
            half.surfaceContainer,
        )
        assertEquals(lerp(light.onTertiaryFixed, dark.onTertiaryFixed, 0.5f), half.onTertiaryFixed)
    }
}
