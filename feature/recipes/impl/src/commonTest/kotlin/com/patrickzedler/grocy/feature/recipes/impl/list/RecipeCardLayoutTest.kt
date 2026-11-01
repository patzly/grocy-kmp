package com.patrickzedler.grocy.feature.recipes.impl.list

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeCardLayoutTest {

    @Test
    fun theMenuButtonIsCenteredOnTheTitlesFirstLine() {
        val offset = firstLineCenterOffset(lineHeight = 24, height = 48)

        assertEquals(-12, offset)
        assertEquals(24 / 2, offset + 48 / 2)
    }

    @Test
    fun aLargerFontMovesTheMenuButtonDownWithTheLine() {
        val offset = firstLineCenterOffset(lineHeight = 48, height = 48)

        assertEquals(0, offset)
    }
}
