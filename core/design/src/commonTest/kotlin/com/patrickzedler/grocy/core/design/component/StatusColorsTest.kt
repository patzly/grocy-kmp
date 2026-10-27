package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class StatusColorsTest {

    private val primary = Color(Hct.from(154.0, 48.0, 40.0).toInt())

    private fun hueOf(color: Color) = Hct.fromInt(color.toArgb()).hue

    private fun hueDistance(a: Double, b: Double) = abs((b - a + 540) % 360 - 180)

    private fun contrast(a: Color, b: Color): Float {
        val lighter = max(a.luminance(), b.luminance())
        val darker = min(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun textOnEveryStatusContainerIsEasyToRead() {
        for (isDark in listOf(false, true)) {
            for (base in listOf(StatusBlue, StatusYellow, StatusRed)) {
                val color = statusColorOf(base, primary, isDark)

                // WCAG AAA for normal text, like the theme's container roles
                assertTrue(contrast(color.container, color.content) >= 7f, "$base dark=$isDark")
            }
        }
    }

    @Test
    fun marksStandOutFromTheSurface() {
        val lightSurface = Color.White
        val darkSurface = Color(0xFF111411)
        for (base in listOf(StatusBlue, StatusYellow, StatusRed)) {
            val light = statusColorOf(base, primary, isDark = false).color
            val dark = statusColorOf(base, primary, isDark = true).color

            // WCAG's 3:1 for graphics, like the theme's primary on its surface
            assertTrue(contrast(light, lightSurface) >= 3f, "$base light")
            assertTrue(contrast(dark, darkSurface) >= 3f, "$base dark")
        }
    }

    @Test
    fun lightContainersAreLightAndDarkOnesDark() {
        val light = statusColorOf(StatusBlue, primary, isDark = false)
        val dark = statusColorOf(StatusBlue, primary, isDark = true)

        assertTrue(light.container.luminance() > 0.5f)
        assertTrue(dark.container.luminance() < 0.2f)
    }

    @Test
    fun colorsTurnTowardsPrimaryAndKeepTheirHue() {
        for (base in listOf(StatusBlue, StatusYellow, StatusRed)) {
            val turned = hueOf(statusColorOf(base, primary, isDark = false).container)

            // Material's harmonization turns by at most 15 degrees
            assertTrue(hueDistance(hueOf(base), turned) <= 16.0, "$base")
            assertTrue(
                hueDistance(turned, hueOf(primary)) < hueDistance(hueOf(base), hueOf(primary)),
                "$base",
            )
        }
    }
}
