package com.patrickzedler.grocy.apps.shared.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.patrickzedler.grocy.core.design.ColorRoleSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WidgetColorsTest {

    private val light = lightColorScheme(primary = Color(0xFF446732))
    private val dark = darkColorScheme(primary = Color(0xFFA9D290))

    @Test
    fun anEventHasItsColorHarmonizedForLightAndDark() {
        val day = eventColorOf("#e57373", light, ColorRoleSpec(isDark = false))
        val night = eventColorOf("#e57373", dark, ColorRoleSpec(isDark = true))

        assertNotEquals(Color(0xFFE57373), day.container)
        assertNotEquals(day.container, night.container)
    }

    @Test
    fun anEventWithoutAValidColorUsesTheSecondaryColor() {
        val roles = eventColorOf("", light, ColorRoleSpec(isDark = false))

        assertEquals(light.secondary, roles.container)
        assertEquals(light.onSecondary, roles.content)
    }
}
