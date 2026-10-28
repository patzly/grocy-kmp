package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_logo_basket
import com.patrickzedler.grocy.core.resources.ic_logo_basket_outline
import com.patrickzedler.grocy.core.resources.ic_logo_citrus
import com.patrickzedler.grocy.core.resources.ic_logo_handle
import com.patrickzedler.grocy.core.resources.ic_logo_letter
import com.patrickzedler.grocy.core.resources.ic_logo_melon
import com.patrickzedler.grocy.core.resources.ic_logo_tomatoes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Grocy's logo in the theme's colors: its layers are stacked and tinted like the splash screen
 * tints them with the system's color roles, the fruits keep their colors harmonized with primary.
 * Decorative, the text beside it names the app.
 */
@Composable
fun GrocyLogo(modifier: Modifier = Modifier, size: Dp = GrocyLogoSize) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) { grocyLogoColors(colorScheme) }
    Box(modifier = modifier.size(size)) {
        LogoLayer(Res.drawable.ic_logo_citrus, colors.citrus)
        LogoLayer(Res.drawable.ic_logo_tomatoes, colors.tomatoes)
        LogoLayer(Res.drawable.ic_logo_melon, colors.melon)
        LogoLayer(Res.drawable.ic_logo_handle, colors.outline)
        LogoLayer(Res.drawable.ic_logo_basket, colors.basket)
        LogoLayer(Res.drawable.ic_logo_basket_outline, colors.outline)
        LogoLayer(Res.drawable.ic_logo_letter, colors.letter)
    }
}

/**
 * [GrocyLogo] in the navigation icon's place of an app bar whose screen has no back button, as
 * far from the bar's start edge as from its top and bottom.
 */
@Composable
fun AppBarLogo(modifier: Modifier = Modifier) {
    GrocyLogo(modifier = modifier.padding(start = AppBarLogoStartPadding))
}

@Composable
private fun LogoLayer(layer: DrawableResource, color: Color) {
    Image(
        painter = painterResource(layer),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        colorFilter = ColorFilter.tint(color),
    )
}

/** The color of each layer of [GrocyLogo]. */
@Immutable
internal data class GrocyLogoColors(
    val citrus: Color,
    val tomatoes: Color,
    val melon: Color,
    val outline: Color,
    val basket: Color,
    val letter: Color,
)

/** The splash screen's colors with the roles of [colorScheme] instead of the system's. */
internal fun grocyLogoColors(colorScheme: ColorScheme) = GrocyLogoColors(
    citrus = LogoYellow.harmonize(colorScheme.primary),
    tomatoes = LogoRed.harmonize(colorScheme.primary),
    melon = LogoGreen.harmonize(colorScheme.primary),
    outline = colorScheme.onSurface,
    basket = colorScheme.primaryContainer,
    letter = colorScheme.onPrimaryContainer.copy(alpha = LETTER_ALPHA),
)

// The fruits of the splash screen (logo_yellow, logo_red, logo_green of apps:shared)
internal val LogoYellow = Color(0xFFE8C349)
internal val LogoRed = Color(0xFFDB716F)
internal val LogoGreen = Color(0xFFA8D473)

private const val LETTER_ALPHA = 0.8f

// The 24.dp drawable scaled up to the size the app bar shows it in, a little smaller than a
// button's 40.dp container
private val GrocyLogoSize = 30.dp

// The drawn logo (23.4 x 18.25 of 24) leaves 21.dp above and below it in the 64.dp bar; the bar
// already insets its navigation icon by 4.dp
private val AppBarLogoStartPadding = 16.dp
