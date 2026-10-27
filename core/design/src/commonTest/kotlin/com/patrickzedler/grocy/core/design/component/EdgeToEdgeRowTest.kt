package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeToEdgeRowTest {

    @Test
    fun rowThatFitsBetweenTheEdgesIsCenteredOnTheScreen() {
        val placement = edgeToEdgeRowPlacement(
            rowWidth = 900,
            viewportWidth = 1600,
            edgeStart = 16,
            edgeEnd = 16,
            contentStart = 480,
            contentEnd = 480,
        )

        assertEquals(EdgeToEdgeRowPlacement(width = 1600, start = 350), placement)
    }

    @Test
    fun rowWiderThanTheScreenStartsWithTheItemsAndScrolls() {
        val placement = edgeToEdgeRowPlacement(
            rowWidth = 900,
            viewportWidth = 400,
            edgeStart = 16,
            edgeEnd = 16,
            contentStart = 16,
            contentEnd = 16,
        )

        assertEquals(EdgeToEdgeRowPlacement(width = 932, start = 16), placement)
    }

    @Test
    fun rowThatOnlyFitsWithoutTheEdgesScrolls() {
        val placement = edgeToEdgeRowPlacement(
            rowWidth = 380,
            viewportWidth = 400,
            edgeStart = 16,
            edgeEnd = 16,
            contentStart = 16,
            contentEnd = 16,
        )

        assertEquals(EdgeToEdgeRowPlacement(width = 412, start = 16), placement)
    }
}
