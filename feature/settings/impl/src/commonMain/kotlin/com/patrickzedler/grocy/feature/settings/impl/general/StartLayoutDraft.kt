package com.patrickzedler.grocy.feature.settings.impl.general

import com.patrickzedler.grocy.core.model.start.StartLayout
import com.patrickzedler.grocy.core.model.start.StartLayoutEntry
import com.patrickzedler.grocy.core.model.start.StartSection

/** A row of the dialog that can move; [key] follows it wherever it goes. */
internal sealed interface StartLayoutRow {
    val key: String

    data class Section(val section: StartSection) : StartLayoutRow {
        override val key: String = section.name
    }

    data class Divider(val id: Int) : StartLayoutRow {
        override val key: String = "divider-$id"
    }
}

/**
 * The arrangement the dialog edits: [shown] is what the start screen will show. Every other
 * section of [sections] is available, below the divider that is always available too.
 */
internal data class StartLayoutDraft(
    val sections: List<StartSection>,
    val shown: List<StartLayoutRow>,
    val nextDividerId: Int,
) {
    /** The divider to take from, the first of the available rows. */
    val divider: StartLayoutRow.Divider
        get() = StartLayoutRow.Divider(nextDividerId)

    val available: List<StartLayoutRow>
        get() = listOf<StartLayoutRow>(divider) + sections
            .filter { StartLayoutRow.Section(it) !in shown }
            .map { StartLayoutRow.Section(it) }

    fun isShown(key: String): Boolean = shown.any { it.key == key }

    /** [key] at [index] of [shown], where it may come from the available rows. */
    fun placed(key: String, index: Int): StartLayoutDraft {
        val row = shown.firstOrNull { it.key == key }
            ?: available.firstOrNull { it.key == key }
            ?: return this
        val others = shown.filter { it.key != key }
        val new = others.take(index) + row + others.drop(index)
        return copy(
            shown = new,
            // The divider just taken leaves a new one behind
            nextDividerId = if (row == divider) nextDividerId + 1 else nextDividerId,
        )
    }

    fun removed(key: String): StartLayoutDraft = copy(shown = shown.filter { it.key != key })

    /** One place up ([by] -1) or down ([by] 1) within [shown]. */
    fun moved(key: String, by: Int): StartLayoutDraft {
        val index = shown.indexOfFirst { it.key == key }
        val target = index + by
        return if (index < 0 || target !in shown.indices) this else placed(key, target)
    }

    /** At the end of what the start screen shows. */
    fun added(key: String): StartLayoutDraft = placed(key, shown.size)

    fun toLayout(): StartLayout = StartLayout(
        shown.map { row ->
            when (row) {
                is StartLayoutRow.Divider -> StartLayoutEntry.Divider
                is StartLayoutRow.Section -> StartLayoutEntry.Section(row.section)
            }
        },
    )

    companion object {
        /** [layout] restricted to the [sections] the server has. */
        fun of(layout: StartLayout, sections: List<StartSection>): StartLayoutDraft {
            var dividers = 0
            val shown = layout.entries.mapNotNull { entry ->
                when (entry) {
                    StartLayoutEntry.Divider -> StartLayoutRow.Divider(dividers++)
                    is StartLayoutEntry.Section ->
                        StartLayoutRow.Section(entry.section).takeIf { entry.section in sections }
                }
            }
            return StartLayoutDraft(sections, shown, dividers)
        }
    }
}

/**
 * Where a row dragged to [center] belongs among the [others] of what is shown, by the middles
 * of those on the screen ([middles], in the list's order); null when none of them is.
 */
internal fun dropIndex(others: List<String>, middles: Map<String, Float>, center: Float): Int? {
    val next = others.firstOrNull { key -> middles[key]?.let { it > center } == true }
    if (next != null) return others.indexOf(next)
    val last = others.lastOrNull { it in middles } ?: return null
    return others.indexOf(last) + 1
}
