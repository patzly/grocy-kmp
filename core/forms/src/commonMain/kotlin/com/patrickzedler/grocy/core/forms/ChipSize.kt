package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How large field and filter chips are. */
enum class ChipSize {
    /** Material's chips. */
    DEFAULT,

    /** Chips with title medium text, readable at arm's length, e.g. in the shopping mode. */
    LARGE,
}

/** The size of every field and filter chip inside, [ChipSize.DEFAULT] unless provided. */
val LocalChipSize: ProvidableCompositionLocal<ChipSize> =
    staticCompositionLocalOf { ChipSize.DEFAULT }

/** The text of a chip: [default] (what the chip itself would use) or title medium. */
@Composable
internal fun chipTextStyle(default: TextStyle = LocalTextStyle.current): TextStyle =
    when (LocalChipSize.current) {
        ChipSize.DEFAULT -> default
        ChipSize.LARGE -> MaterialTheme.typography.titleMedium
    }

/** A chip's height, Material's 32.dp or more room for the larger text. */
@Composable
internal fun chipHeight(): Dp = when (LocalChipSize.current) {
    ChipSize.DEFAULT -> DefaultChipHeight
    ChipSize.LARGE -> LargeChipHeight
}

/** The size of a chip's icons. */
@Composable
internal fun chipIconSize(default: Dp): Dp = when (LocalChipSize.current) {
    ChipSize.DEFAULT -> default
    ChipSize.LARGE -> LargeChipIconSize
}

/** Raises a Material chip to [chipHeight]; Material's own chips keep 32.dp at least. */
@Composable
internal fun Modifier.chipMinHeight(): Modifier = when (LocalChipSize.current) {
    ChipSize.DEFAULT -> this
    ChipSize.LARGE -> heightIn(min = LargeChipHeight)
}

private val DefaultChipHeight = 32.dp
private val LargeChipHeight = 40.dp
private val LargeChipIconSize = 20.dp
