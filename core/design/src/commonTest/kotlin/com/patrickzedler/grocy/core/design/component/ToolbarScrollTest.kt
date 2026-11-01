package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolbarScrollTest {

    @Test
    fun aPhoneInLandscapeHidesTheToolbarWhileScrolling() {
        assertTrue(hidesToolbarOnScroll(windowHeight = 411.dp))
    }

    @Test
    fun aPhoneInPortraitKeepsTheToolbar() {
        assertFalse(hidesToolbarOnScroll(windowHeight = 915.dp))
    }

    @Test
    fun theMediumHeightClassKeepsTheToolbarFromItsFirstDp() {
        assertFalse(hidesToolbarOnScroll(windowHeight = 480.dp))
    }
}
