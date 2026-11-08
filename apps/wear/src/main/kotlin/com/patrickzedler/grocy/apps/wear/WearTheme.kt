package com.patrickzedler.grocy.apps.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.google_sans_flex_medium
import com.patrickzedler.grocy.core.resources.google_sans_flex_regular
import com.patrickzedler.grocy.core.wear.WearPalette
import org.jetbrains.compose.resources.Font

/**
 * Material 3 for the watch: the colors of the watch face where the system offers them (Wear OS
 * 5.1 and newer, if the user did not turn them off), otherwise the app's green, always in the
 * phone app's Google Sans Flex.
 */
@Composable
internal fun GrocyWearTheme(content: @Composable () -> Unit) {
    val googleSansFlex = FontFamily(
        Font(Res.font.google_sans_flex_regular, FontWeight.Normal),
        Font(Res.font.google_sans_flex_medium, FontWeight.Medium),
    )
    MaterialTheme(
        colorScheme = dynamicColorScheme(LocalContext.current) ?: GrocyColorScheme,
        typography = remember(googleSansFlex) { Typography(googleSansFlex) },
        content = content,
    )
}

private val GrocyColorScheme = ColorScheme(
    primary = Color(WearPalette.primary),
    primaryDim = Color(WearPalette.primaryDim),
    primaryContainer = Color(WearPalette.primaryContainer),
    onPrimary = Color(WearPalette.onPrimary),
    onPrimaryContainer = Color(WearPalette.onPrimaryContainer),
    secondary = Color(WearPalette.secondary),
    secondaryDim = Color(WearPalette.secondaryDim),
    secondaryContainer = Color(WearPalette.secondaryContainer),
    onSecondary = Color(WearPalette.onSecondary),
    onSecondaryContainer = Color(WearPalette.onSecondaryContainer),
    tertiary = Color(WearPalette.tertiary),
    tertiaryDim = Color(WearPalette.tertiaryDim),
    tertiaryContainer = Color(WearPalette.tertiaryContainer),
    onTertiary = Color(WearPalette.onTertiary),
    onTertiaryContainer = Color(WearPalette.onTertiaryContainer),
    surfaceContainerLow = Color(WearPalette.surfaceContainerLow),
    surfaceContainer = Color(WearPalette.surfaceContainer),
    surfaceContainerHigh = Color(WearPalette.surfaceContainerHigh),
    onSurface = Color(WearPalette.onSurface),
    onSurfaceVariant = Color(WearPalette.onSurfaceVariant),
    outline = Color(WearPalette.outline),
    outlineVariant = Color(WearPalette.outlineVariant),
    background = Color(WearPalette.background),
    onBackground = Color(WearPalette.onBackground),
    error = Color(WearPalette.error),
    errorDim = Color(WearPalette.errorDim),
    errorContainer = Color(WearPalette.errorContainer),
    onError = Color(WearPalette.onError),
    onErrorContainer = Color(WearPalette.onErrorContainer),
)
