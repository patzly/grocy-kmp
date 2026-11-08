package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ToolbarMenuPositionTest {

    private val anchor = IntRect(left = 300, top = 2000, right = 500, bottom = 2100)
    private val menu = IntSize(width = 400, height = 600)

    private fun position(
        anchor: IntRect = this.anchor,
        windowWidth: Int = 1080,
        menu: IntSize = this.menu,
        isRtl: Boolean = false,
    ) = menuPositionAboveAnchor(
        anchor = anchor,
        windowWidth = windowWidth,
        menu = menu,
        isRtl = isRtl,
        distance = 40,
        sideMargin = 30,
        topMargin = 120,
    )

    @Test
    fun theMenuEndsTheDistanceAboveTheAnchor() {
        assertEquals(IntOffset(300, 1360), position())
    }

    @Test
    fun theMenuStaysAtTheToolbarNearTheBottomOfTheWindow() {
        // Material kept 48.dp to a height without the system bars and moved this one up
        val low = IntRect(left = 300, top = 2300, right = 500, bottom = 2400)

        assertEquals(1660, position(anchor = low).y)
    }

    @Test
    fun rightToLeftItStartsAtTheAnchorsRightEdge() {
        assertEquals(100, position(isRtl = true).x)
    }

    @Test
    fun itStaysInsideTheSidesOfTheWindow() {
        assertEquals(30, position(anchor = IntRect(0, 2000, 100, 2100)).x)
        assertEquals(650, position(anchor = IntRect(900, 2000, 1000, 2100)).x)
    }

    @Test
    fun aMenuWiderThanTheWindowIsCentered() {
        assertEquals(-10, position(windowWidth = 380).x)
    }

    @Test
    fun aMenuTooHighForTheWindowKeepsTheTopMargin() {
        assertEquals(120, position(menu = IntSize(400, 5000)).y)
    }
}
