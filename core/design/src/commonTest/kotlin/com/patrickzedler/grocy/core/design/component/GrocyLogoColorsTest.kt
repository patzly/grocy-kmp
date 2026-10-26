package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrocyLogoColorsTest {

    private fun hueOf(color: Color) = Hct.fromInt(color.toArgb()).hue

    private fun hueDistance(a: Double, b: Double) = abs((b - a + 540) % 360 - 180)

    private val scheme = lightColorScheme(
        primary = Color(Hct.from(250.0, 48.0, 40.0).toInt()),
        onSurface = Color(0xFF191D16),
        primaryContainer = Color(0xFFC5EFAB),
        onPrimaryContainer = Color(0xFF2D4F1C),
    )

    @Test
    fun basketTakesTheThemesRolesLikeTheSplashScreenTheSystems() {
        val colors = grocyLogoColors(scheme)

        assertEquals(scheme.onSurface, colors.outline)
        assertEquals(scheme.primaryContainer, colors.basket)
        assertEquals(scheme.onPrimaryContainer.copy(alpha = 0.8f), colors.letter)
    }

    @Test
    fun fruitsTurnTowardsPrimaryWithoutLosingTheirHue() {
        val primaryHue = hueOf(scheme.primary)

        val colors = grocyLogoColors(scheme)

        listOf(
            LogoYellow to colors.citrus,
            LogoRed to colors.tomatoes,
            LogoGreen to colors.melon,
        ).forEach { (fruit, harmonized) ->
            val turned = hueDistance(hueOf(fruit), hueOf(harmonized))
            assertTrue(turned > 0.5 && turned <= 15.5, "turned by $turned")
            assertTrue(
                hueDistance(hueOf(harmonized), primaryHue) < hueDistance(hueOf(fruit), primaryHue),
            )
        }
    }
}
