package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

/**
 * Keeps the screen from turning off while this is shown and [enabled], e.g. while shopping with
 * the list in one hand. Android sets it on the window's view; browsers hold a screen wake lock
 * where they offer one, taken again when the page becomes visible.
 */
@Composable
expect fun KeepScreenOn(enabled: Boolean = true)
