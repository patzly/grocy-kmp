package com.patrickzedler.grocy.apps.wear

import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.Typography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WearThemeTest {

    @Test
    fun everyStyleUsesTheAppsFont() {
        val family = FontFamily.Cursive

        val typography = Typography().withFontFamily(family)

        val styles = with(typography) {
            listOf(
                displayLarge, displayMedium, displaySmall,
                titleLarge, titleMedium, titleSmall,
                labelLarge, labelMedium, labelSmall,
                bodyLarge, bodyMedium, bodySmall, bodyExtraSmall,
                numeralExtraLarge, numeralLarge, numeralMedium, numeralSmall, numeralExtraSmall,
            )
        }
        assertTrue(styles.all { it.fontFamily == family })
        assertEquals(family, typography.arcLarge.fontFamily)
        assertEquals(family, typography.arcMedium.fontFamily)
        assertEquals(family, typography.arcSmall.fontFamily)
    }

    @Test
    fun theDefaultFontParameterWouldLeaveWearsOwnFont() {
        // Why withFontFamily exists; if this fails, Material's parameter does the job now
        assertTrue(Typography(FontFamily.Cursive).bodyLarge.fontFamily != FontFamily.Cursive)
    }

    @Test
    fun theStylesKeepTheirSizes() {
        val typography = Typography()

        assertEquals(
            typography.titleMedium.fontSize,
            typography.withFontFamily(FontFamily.Cursive).titleMedium.fontSize,
        )
    }
}
