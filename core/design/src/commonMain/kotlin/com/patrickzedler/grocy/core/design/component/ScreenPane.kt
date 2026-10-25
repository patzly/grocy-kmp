package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * The pane of a list-detail layout a screen fills. Both panes lie on a surfaceContainerHighest
 * background with a margin at the screen's edges; below its app bar in the background's color
 * each screen has a rounded surfaceContainer surface, the background of single screens, so their
 * content looks the same in every window size.
 */
enum class ScreenPane { List, Detail }

/** The margin at the screen's edges, outside the [ScreenPane]s. */
private val PaneMargin = 16.dp

/** The outer margin of a [ScreenPane] screen: at the start for the list, the end for details. */
internal fun Modifier.paneMargin(pane: ScreenPane): Modifier = padding(
    start = if (pane == ScreenPane.List) PaneMargin else 0.dp,
    end = if (pane == ScreenPane.Detail) PaneMargin else 0.dp,
)

/**
 * The rounded surface of a [ScreenPane] below its app bar. It ends above the navigation bar and
 * the keyboard (both are consumed for the content) with the margin below it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Modifier.paneSurface(): Modifier = this
    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.keyboard))
    .padding(bottom = PaneMargin)
    .clip(MaterialTheme.shapes.extraLargeIncreased)
    .background(MaterialTheme.colorScheme.surfaceContainer)
