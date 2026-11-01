package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScreenPaneTest {

    @Test
    fun aScreenKeepsItsOwnPane() {
        assertEquals(ScreenPane.Detail, shownPane(ScreenPane.Detail, showsPanes = true))
        assertEquals(ScreenPane.List, shownPane(ScreenPane.List, showsPanes = true))
    }

    @Test
    fun aScreenWithoutPaneIsASinglePaneWhereTwoWouldFit() {
        assertEquals(ScreenPane.Single, shownPane(null, showsPanes = true))
    }

    @Test
    fun narrowWindowsShowScreensWithoutPaneAsWholeScreen() {
        assertNull(shownPane(null, showsPanes = false))
    }
}
