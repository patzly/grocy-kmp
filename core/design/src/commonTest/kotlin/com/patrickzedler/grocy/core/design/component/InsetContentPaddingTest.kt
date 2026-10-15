package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class InsetContentPaddingTest {

    @Test
    fun horizontalPaddingUsesTheLargerSafeSideOnBothSides() {
        val padding = InsetContentPadding(
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            safePadding = PaddingValues(start = 48.dp, end = 0.dp),
            horizontalBasePadding = 16.dp,
        )

        assertEquals(64.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(64.dp, padding.calculateRightPadding(LayoutDirection.Rtl))
        assertEquals(8.dp, padding.calculateTopPadding())
        assertEquals(24.dp, padding.calculateBottomPadding())
    }

    @Test
    fun screenPaddingAddsSpacingBelowTheCurrentInsets() {
        val insets = MutableBottomPadding(48.dp)
        val padding = ScreenContentPadding(top = 80.dp, bottomInsets = insets, spacing = 16.dp)

        assertEquals(80.dp, padding.calculateTopPadding())
        assertEquals(64.dp, padding.calculateBottomPadding())

        insets.bottom = 0.dp

        assertEquals(16.dp, padding.calculateBottomPadding())
    }

    /** Insets that change like an animated keyboard, read on every call. */
    private class MutableBottomPadding(var bottom: Dp) : PaddingValues {
        override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
        override fun calculateTopPadding() = 0.dp
        override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
        override fun calculateBottomPadding() = bottom
    }
}
