package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable

/** An item of a lazy list as laid out: its place in the list, key, size and offset. */
internal data class LaidOutItem(val index: Int, val key: Any, val size: Int, val offset: Int)

/**
 * The sizes of a lazy list's items, remembered once laid out, so the scrollbar's length stays
 * put. Compose's own estimate takes the average of the visible items for all of them, which
 * changes with every item of another height scrolling in. Here only items never laid out are
 * estimated, by the average of the known ones; a list whose items changed starts anew.
 */
internal class ItemSizes {
    private val sizes = HashMap<Int, Int>()
    private val keys = HashMap<Int, Any>()
    private var knownSum = 0L
    private var itemCount = -1

    /** Remembers the sizes of [items] of a list with [totalCount] items. */
    fun update(totalCount: Int, items: List<LaidOutItem>) {
        val changed = totalCount != itemCount ||
            items.any { item -> keys[item.index].let { it != null && it != item.key } }
        if (changed) {
            sizes.clear()
            keys.clear()
            knownSum = 0
            itemCount = totalCount
        }
        for (item in items) {
            knownSum += item.size - (sizes[item.index] ?: 0)
            sizes[item.index] = item.size
            keys[item.index] = item.key
        }
    }

    private val averageSize: Double
        get() = if (sizes.isEmpty()) 0.0 else knownSum.toDouble() / sizes.size

    /** The items before [index] with the [spacing] after each of them. */
    fun sizeBefore(index: Int, spacing: Int): Int {
        var known = 0L
        var knownCount = 0
        for ((i, size) in sizes) {
            if (i < index) {
                known += size
                knownCount++
            }
        }
        return (known + (index - knownCount) * averageSize + index.toLong() * spacing).toInt()
    }

    /** All items with the [spacing] between them. */
    fun totalSize(spacing: Int): Int {
        if (itemCount <= 0) return 0
        val unknown = itemCount - sizes.size
        return (knownSum + unknown * averageSize + (itemCount - 1).toLong() * spacing).toInt()
    }
}

/**
 * The scroll position of a lazy list for its scrollbar, from the remembered sizes of its items
 * ([ItemSizes]): how far [items] (the laid out ones) are scrolled and how long the content is,
 * content padding included.
 */
internal class ScrollPosition(val scrollOffset: Int, val contentSize: Int)

internal fun ItemSizes.positionOf(
    totalCount: Int,
    items: List<LaidOutItem>,
    spacing: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
): ScrollPosition {
    update(totalCount, items)
    val first = items.minByOrNull { it.index }
        ?: return ScrollPosition(0, beforeContentPadding + afterContentPadding)
    return ScrollPosition(
        scrollOffset = sizeBefore(first.index, spacing) - first.offset,
        contentSize = beforeContentPadding + totalSize(spacing) + afterContentPadding,
    )
}

/** [ScrollIndicatorState] of [state] with the lengths of [ItemSizes]. */
@Stable
internal class LazyListScrollIndicatorState(
    private val state: LazyListState,
) : ScrollIndicatorState {
    private val sizes = ItemSizes()

    private fun position(info: LazyListLayoutInfo) = sizes.positionOf(
        totalCount = info.totalItemsCount,
        items = info.visibleItemsInfo.map { LaidOutItem(it.index, it.key, it.size, it.offset) },
        spacing = info.mainAxisItemSpacing,
        beforeContentPadding = info.beforeContentPadding,
        afterContentPadding = info.afterContentPadding,
    )

    override val scrollOffset: Int
        get() {
            val position = position(state.layoutInfo)
            // Estimated sizes of items not laid out yet may not match the real end
            val max = (position.contentSize - viewportSize).coerceAtLeast(0)
            return position.scrollOffset.coerceIn(0, max)
        }

    override val contentSize: Int
        get() = position(state.layoutInfo).contentSize

    override val viewportSize: Int
        get() = state.layoutInfo.viewportSize.height
}
