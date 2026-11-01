package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals

class LazyListScrollIndicatorTest {

    private fun item(index: Int, size: Int, offset: Int, key: Any = index) =
        LaidOutItem(index, key, size, offset)

    @Test
    fun contentSizeStaysWhenAnItemOfAnotherHeightScrollsIn() {
        val sizes = ItemSizes()
        val atTop = sizes.positionOf(4, listOf(item(0, 100, 0), item(1, 100, 100)), 0, 0, 0)

        // A tall item scrolls in, the short ones scroll back
        sizes.positionOf(4, listOf(item(1, 100, -50), item(2, 300, 50)), 0, 0, 0)
        val back = sizes.positionOf(4, listOf(item(0, 100, 0), item(1, 100, 100)), 0, 0, 0)

        assertEquals(400, atTop.contentSize)
        // Known now: 100 + 100 + 300, the last one estimated by their average
        assertEquals(666, back.contentSize)
        assertEquals(0, back.scrollOffset)
    }

    @Test
    fun theLengthNoLongerChangesOnceEveryItemWasLaidOut() {
        val sizes = ItemSizes()
        sizes.positionOf(3, listOf(item(0, 100, 0), item(1, 50, 100)), 0, 0, 0)
        val end = sizes.positionOf(3, listOf(item(1, 50, 0), item(2, 400, 50)), 0, 0, 0)

        val again = sizes.positionOf(3, listOf(item(0, 100, -20), item(1, 50, 80)), 0, 0, 0)

        assertEquals(550, end.contentSize)
        assertEquals(550, again.contentSize)
        assertEquals(20, again.scrollOffset)
    }

    @Test
    fun scrollOffsetCountsTheItemsBeforeWithTheirSpacing() {
        val sizes = ItemSizes()
        sizes.positionOf(3, listOf(item(0, 100, 0), item(1, 50, 108)), 8, 16, 16)

        val position = sizes.positionOf(3, listOf(item(1, 50, -10), item(2, 70, 48)), 8, 16, 16)

        // 100 + 8 before the second item, scrolled 10 into it
        assertEquals(118, position.scrollOffset)
        assertEquals(16 + 100 + 8 + 50 + 8 + 70 + 16, position.contentSize)
    }

    @Test
    fun otherItemsAtTheSameIndicesStartAnew() {
        val sizes = ItemSizes()
        sizes.positionOf(2, listOf(item(0, 100, 0), item(1, 900, 100)), 0, 0, 0)

        // A filter shows other items, as many as before
        val filtered = sizes.positionOf(
            2,
            listOf(item(0, 100, 0, key = "a"), item(1, 100, 100, key = "b")),
            0,
            0,
            0,
        )

        assertEquals(200, filtered.contentSize)
    }

    @Test
    fun anEmptyListIsItsPadding() {
        assertEquals(32, ItemSizes().positionOf(0, emptyList(), 0, 16, 16).contentSize)
    }
}
