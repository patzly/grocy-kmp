package com.patrickzedler.grocy.feature.batteries.impl

import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalField
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalSortField
import com.patrickzedler.grocy.core.model.batteries.BatteryChargeCycle
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.monthsBefore

/** Grocy's date ranges of the batteries journal in months, the last one for everything. */
internal val BatteriesJournalRanges = listOf(1, 6, 12, 24, BATTERIES_JOURNAL_ALL_MONTHS)

internal const val BATTERIES_JOURNAL_DEFAULT_MONTHS = 24
internal const val BATTERIES_JOURNAL_ALL_MONTHS = 9999

/** The first day of the range of [months] before [today] like Grocy's, null for everything. */
internal fun batteriesJournalSince(today: Long, months: Int): String? =
    if (months >= BATTERIES_JOURNAL_ALL_MONTHS) null else monthsBefore(today, months)

/** Grocy's filters of the batteries journal. */
internal data class BatteriesJournalFilter(
    val batteryId: Int? = null,
    val months: Int = BATTERIES_JOURNAL_DEFAULT_MONTHS,
) {
    val isActive: Boolean
        get() = batteryId != null || months != BATTERIES_JOURNAL_DEFAULT_MONTHS
}

/** One row of Grocy's batteries journal. */
internal data class BatteriesJournalItem(
    val cycle: BatteryChargeCycle,
    val batteryName: String,
    /** The userfields Grocy shows as table columns with a value, as caption and text. */
    val userfields: List<Pair<String, String>>,
)

/** Rows below a group title; [title] is null without grouping. */
internal data class BatteriesJournalGroup(
    val title: String?,
    val rows: List<BatteriesJournalItem>,
)

/** The columns the server's userfields [hasUserfieldColumns] offer. */
internal fun availableBatteriesJournalFields(
    hasUserfieldColumns: Boolean,
): List<BatteriesJournalField> = BatteriesJournalField.entries.filter {
    it != BatteriesJournalField.USERFIELDS || hasUserfieldColumns
}

/** The battery and the shown columns, what the journal can be sorted by. */
internal fun availableBatteriesJournalSortFields(
    fields: Collection<BatteriesJournalField>,
): List<BatteriesJournalSortField> = BatteriesJournalSortField.entries.filter {
    it == BatteriesJournalSortField.BATTERY || BatteriesJournalField.TRACKED_TIME in fields
}

/** The order the journal has: the chosen one, the default while its column is hidden. */
internal fun effectiveBatteriesJournalSort(
    options: BatteriesJournalOptions,
    fields: Collection<BatteriesJournalField>,
): Pair<BatteriesJournalSortField, Boolean> {
    if (options.sortField in availableBatteriesJournalSortFields(fields)) {
        return options.sortField to options.ascending
    }
    return defaultBatteriesJournalSort(fields)
}

/** Grocy's default: newest tracked time first, by battery while that column is hidden. */
internal fun defaultBatteriesJournalSort(
    fields: Collection<BatteriesJournalField>,
): Pair<BatteriesJournalSortField, Boolean> =
    if (BatteriesJournalField.TRACKED_TIME in fields) {
        BatteriesJournalSortField.TRACKED_TIME to false
    } else {
        BatteriesJournalSortField.BATTERY to true
    }

/** The rows of Grocy's batteries journal, filtered and searched, sorted and grouped. */
internal fun buildBatteriesJournal(
    cycles: List<BatteryChargeCycle>,
    batteries: List<Battery>,
    userfields: List<Userfield>,
    filter: BatteriesJournalFilter,
    query: String,
    sort: Pair<BatteriesJournalSortField, Boolean> =
        BatteriesJournalSortField.TRACKED_TIME to false,
    grouping: BatteriesJournalGrouping = BatteriesJournalGrouping.NONE,
): List<BatteriesJournalGroup> {
    val names = batteries.associate { it.id to it.name }
    val columns = userfields.filter { it.showAsColumnInTables }
    val items = cycles
        .filter { filter.batteryId == null || it.batteryId == filter.batteryId }
        .mapNotNull { cycle ->
            // Charge cycles of deleted batteries have no name to show, like Grocy's table
            val name = names[cycle.batteryId] ?: return@mapNotNull null
            BatteriesJournalItem(
                cycle = cycle,
                batteryName = name,
                userfields = columns.mapNotNull { field ->
                    userfieldText(field, cycle.userfields[field.name])
                        ?.let { field.caption to it }
                },
            )
        }
    val rows = sorted(search(items, query), sort)
    return when (grouping) {
        BatteriesJournalGrouping.NONE ->
            if (rows.isEmpty()) emptyList() else listOf(BatteriesJournalGroup(null, rows))
        BatteriesJournalGrouping.BATTERY -> rows.groupBy { it.batteryName }.entries
            .sortedBy { it.key.lowercase() }
            .map { (name, group) -> BatteriesJournalGroup(name, group) }
    }
}

private fun search(
    items: List<BatteriesJournalItem>,
    query: String,
): List<BatteriesJournalItem> {
    if (query.isBlank()) return items
    val matches = FuzzySearch.filter(items, query) { item ->
        listOf(item.batteryName) + item.userfields.map { it.second }
    }.map { it.cycle.id }.toSet()
    return items.filter { it.cycle.id in matches }
}

private fun sorted(
    items: List<BatteriesJournalItem>,
    sort: Pair<BatteriesJournalSortField, Boolean>,
): List<BatteriesJournalItem> {
    val (field, ascending) = sort
    val order = when (field) {
        BatteriesJournalSortField.BATTERY ->
            compareBy<BatteriesJournalItem> { it.batteryName.lowercase() }
        BatteriesJournalSortField.TRACKED_TIME ->
            compareBy { it.cycle.trackedTime }
    }
    return items.sortedWith(if (ascending) order else order.reversed())
}
