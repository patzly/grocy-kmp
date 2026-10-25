package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.google_sans_flex_medium
import com.patrickzedler.grocy.core.resources.google_sans_flex_regular
import org.jetbrains.compose.resources.Font

/**
 * Material typography in Google Sans Flex.
 *
 * Composable because Compose Resources load fonts asynchronously on the web.
 */
@Composable
internal fun grocyTypography(): Typography {
    val googleSansFlex = FontFamily(
        Font(Res.font.google_sans_flex_regular, FontWeight.Normal),
        Font(Res.font.google_sans_flex_medium, FontWeight.Medium),
    )
    return remember(googleSansFlex) { Typography().withFontFamily(googleSansFlex) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Typography.withFontFamily(fontFamily: FontFamily): Typography {
    fun TextStyle.regular() = copy(fontFamily = fontFamily)
    fun TextStyle.emphasized() = copy(fontFamily = fontFamily, fontWeight = FontWeight.Medium)

    return Typography(
        displayLarge = displayLarge.regular(),
        displayMedium = displayMedium.regular(),
        displaySmall = displaySmall.regular(),
        headlineLarge = headlineLarge.regular(),
        headlineMedium = headlineMedium.regular(),
        headlineSmall = headlineSmall.regular(),
        titleLarge = titleLarge.regular(),
        titleMedium = titleMedium.regular(),
        titleSmall = titleSmall.regular(),
        bodyLarge = bodyLarge.regular(),
        bodyMedium = bodyMedium.regular(),
        bodySmall = bodySmall.regular(),
        labelLarge = labelLarge.regular(),
        labelMedium = labelMedium.regular(),
        labelSmall = labelSmall.regular(),
        displayLargeEmphasized = displayLargeEmphasized.emphasized(),
        displayMediumEmphasized = displayMediumEmphasized.emphasized(),
        displaySmallEmphasized = displaySmallEmphasized.emphasized(),
        headlineLargeEmphasized = headlineLargeEmphasized.emphasized(),
        headlineMediumEmphasized = headlineMediumEmphasized.emphasized(),
        headlineSmallEmphasized = headlineSmallEmphasized.emphasized(),
        titleLargeEmphasized = titleLargeEmphasized.emphasized(),
        titleMediumEmphasized = titleMediumEmphasized.emphasized(),
        titleSmallEmphasized = titleSmallEmphasized.emphasized(),
        bodyLargeEmphasized = bodyLargeEmphasized.emphasized(),
        bodyMediumEmphasized = bodyMediumEmphasized.emphasized(),
        bodySmallEmphasized = bodySmallEmphasized.emphasized(),
        labelLargeEmphasized = labelLargeEmphasized.emphasized(),
        labelMediumEmphasized = labelMediumEmphasized.emphasized(),
        labelSmallEmphasized = labelSmallEmphasized.emphasized(),
    )
}

/**
 * The style with tabular figures, every digit equally wide: version numbers and other numbers
 * read as columns, a "1" no longer narrower than a "5". Keeps font features set already.
 */
fun TextStyle.withTabularNumbers(): TextStyle {
    val features = fontFeatureSettings?.takeIf { it.isNotBlank() }
    if (features?.split(',')?.any { it.trim() == TABULAR_NUMBERS } == true) return this
    return copy(fontFeatureSettings = listOfNotNull(features, TABULAR_NUMBERS).joinToString(", "))
}

private const val TABULAR_NUMBERS = "tnum"
