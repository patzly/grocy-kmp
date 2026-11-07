package com.patrickzedler.grocy.core.design

import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.toHct
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class CustomColorsTest {

    private fun tone(color: Color) = color.toHct().tone

    private fun assertSameTone(expected: Color, actual: Color, message: String) =
        assertTrue(
            abs(tone(expected) - tone(actual)) < 1.5,
            "$message: ${tone(expected)} ${tone(actual)}",
        )

    @Test
    fun aCustomColorOfPrimarysHueHasExactlyThePrimaryRoles() {
        for (hue in listOf(20f, 154f, 250f)) {
            for (contrast in AppContrast.entries) {
                for (isDark in listOf(false, true)) {
                    val appearance = AppearanceSettings(hue = hue, contrast = contrast)
                    val scheme = seededColorScheme(appearance, isDark)
                    val spec = colorRoleSpecOf(appearance, isDark)

                    val roles = customColorRoles(scheme.primary.toHct().hue, scheme.primary, spec)

                    val case = "hue $hue $contrast dark $isDark"
                    assertSameTone(scheme.primary, roles.color, "$case color")
                    assertSameTone(scheme.onPrimary, roles.onColor, "$case on color")
                    assertSameTone(scheme.primaryContainer, roles.container, "$case container")
                    assertSameTone(
                        scheme.onPrimaryContainer,
                        roles.onContainer,
                        "$case on container",
                    )
                }
            }
        }
    }

    @Test
    fun higherContrastMovesContentFurtherFromItsContainer() {
        val primary = seededPrimaryColor(AppearanceSettings(), isDark = false)

        val standard = customColorRoles(250.0, primary, ColorRoleSpec(isDark = false))
        val high = customColorRoles(
            250.0,
            primary,
            ColorRoleSpec(isDark = false, contrastLevel = 1.0),
        )

        assertTrue(
            abs(tone(high.container) - tone(high.onContainer)) >
                abs(tone(standard.container) - tone(standard.onContainer)),
        )
    }

    @Test
    fun onlyTheHueIsChosenTheChromaIsPrimarys() {
        val primary = seededPrimaryColor(AppearanceSettings(hue = 154f), isDark = false)

        val roles = customColorRoles(250.0, primary, ColorRoleSpec(isDark = false))

        val color = roles.color.toHct()
        assertTrue(abs(color.hue - 250.0) < 2.0, "${color.hue}")
        assertTrue(abs(color.chroma - primary.toHct().chroma) < 2.0, "${color.chroma}")
    }

    @Test
    fun darkSchemesGetLightColorsAndDarkContainers() {
        val primary = seededPrimaryColor(AppearanceSettings(), isDark = true)

        val light = customColorRoles(30.0, primary, ColorRoleSpec(isDark = false))
        val dark = customColorRoles(30.0, primary, ColorRoleSpec(isDark = true))

        assertTrue(tone(dark.color) > tone(light.color))
        assertTrue(tone(dark.container) < tone(light.container))
    }
}
