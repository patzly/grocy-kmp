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

    private val baseHues = listOf(GREEN_HUE, BLUE_HUE, YELLOW_HUE, RED_HUE)

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
            for (base in baseHues) {
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
        for (base in baseHues) {
            val light = statusColorOf(base, primary, isDark = false).color
            val dark = statusColorOf(base, primary, isDark = true).color

            // WCAG's 3:1 for graphics, like the theme's primary on its surface
            assertTrue(contrast(light, lightSurface) >= 3f, "$base light")
            assertTrue(contrast(dark, darkSurface) >= 3f, "$base dark")
        }
    }

    @Test
    fun lightContainersAreLightAndDarkOnesDark() {
        val light = statusColorOf(BLUE_HUE, primary, isDark = false)
        val dark = statusColorOf(BLUE_HUE, primary, isDark = true)

        assertTrue(light.container.luminance() > 0.5f)
        assertTrue(dark.container.luminance() < 0.2f)
    }

    @Test
    fun colorsTurnTowardsPrimaryAndKeepTheirHue() {
        for (base in baseHues) {
            val turned = hueOf(statusColorOf(base, primary, isDark = false).container)

            // Material's harmonization turns by at most 15 degrees
            assertTrue(hueDistance(base, turned) <= 16.0, "$base")
            assertTrue(
                hueDistance(turned, hueOf(primary)) < hueDistance(base, hueOf(primary)),
                "$base",
            )
        }
    }

    @Test
    fun everyStatusIsExactlyAsColorfulAsPrimary() {
        val primaryChroma = Hct.fromInt(primary.toArgb()).chroma
        for (base in baseHues) {
            val color = Hct.fromInt(statusColorOf(base, primary, isDark = false).color.toArgb())

            // Unless sRGB has no color that colorful at this hue and tone
            val reachable = Hct.from(color.hue, 200.0, color.tone).chroma
            val expected = min(primaryChroma, reachable)
            // Rounding to 8 bit sRGB moves the chroma a little
            assertTrue(abs(color.chroma - expected) < 1.0, "$base: ${color.chroma}")
        }
    }

    @Test
    fun yellowStaysYellowInsteadOfAmber() {
        // Amber lies below 80 degrees, harmonizing only turns it towards the green primary
        val yellow = hueOf(statusColorOf(YELLOW_HUE, primary, isDark = false).container)

        assertTrue(yellow in 88.0..106.0, "$yellow")
    }

    @Test
    fun aCalmerThemeGivesCalmerStatusColors() {
        val calmPrimary = Color(Hct.from(154.0, 16.0, 40.0).toInt())

        val color = statusColorOf(GREEN_HUE, calmPrimary, isDark = false).color
        val chroma = Hct.fromInt(color.toArgb()).chroma

        assertTrue(abs(chroma - 16.0) < 1.0, "$chroma")
    }

    @Test
    fun customColorsKeepTheirContentReadable() {
        val colors = listOf(
            Color(0xFF007BFF),
            Color(0xFF28A745),
            Color(0xFFFFC107),
            Color(0xFF17A2B8),
            Color(0xFF6C757D),
            Color.Black,
            Color.White,
        )
        colors.forEach { color ->
            listOf(false, true).forEach { isDark ->
                val roles = customStatusColorOf(color, primary, isDark)

                assertTrue(contrast(roles.container, roles.content) >= 4.5f, "$color dark=$isDark")
            }
        }
    }

    @Test
    fun customColorsTurnTowardsTheTheme() {
        val red = Color(0xFFDC3545)

        val roles = customStatusColorOf(red, primary, isDark = false)

        assertTrue(hueDistance(hueOf(roles.color), hueOf(primary)) < hueDistance(hueOf(red), hueOf(primary)))
    }

    @Test
    fun aHarmonizedColorStaysRecognizable() {
        val blue = Color(0xFF007BFF)

        val roles = customStatusColorOf(blue, primary, isDark = true)

        assertTrue(hueDistance(hueOf(roles.color), hueOf(blue)) <= 20.0)
    }
}
