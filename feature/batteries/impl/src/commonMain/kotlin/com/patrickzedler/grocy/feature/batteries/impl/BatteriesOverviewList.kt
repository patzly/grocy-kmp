package com.patrickzedler.grocy.feature.batteries.impl

import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.forms.userfieldTextOrder
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewField
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewSortField
import com.patrickzedler.grocy.core.model.batteries.BatteryDueStatus
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.model.batteries.batteryDueStatusOf
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.text.FuzzySearch

/** One row of Grocy's batteries overview. */
internal data class BatteryRow(
    val current: CurrentBattery,
    val battery: Battery,
    val dueStatus: BatteryDueStatus?,
    /** The userfields Grocy shows as table columns with a value, as caption and text. */
    val userfields: List<Pair<String, String>>,
    /** Their texts by the field's name, what sorting and grouping by them use. */
    val userfieldTexts: Map<String, String> = emptyMap(),
)

/** Rows below a group title; [title] is null without grouping and for rows without a value. */
internal data class BatteryGroup(
    val title: String?,
    val rows: List<BatteryRow>,
    val isUnassigned: Boolean = false,
)

/** What the batteries overview's list is built from. */
internal class BatteriesOverviewData(
    val current: List<CurrentBattery>,
    val batteries: List<Battery>,
    val userfields: List<Userfield>,
)

/** The columns the server's userfields [hasUserfieldColumns] offer. */
internal fun availableBatteryFields(hasUserfieldColumns: Boolean): List<BatteriesOverviewField> =
    BatteriesOverviewField.entries.filter {
        it != BatteriesOverviewField.USERFIELDS || hasUserfieldColumns
    }

/** "Used in", Grocy's only grouping of its own, and the server's [userfieldColumns]. */
internal fun availableBatteryGroupings(
    userfieldColumns: List<Userfield>,
): List<TableColumn<BatteriesOverviewGrouping>> =
    BatteriesOverviewGrouping.entries.map { TableColumn.Own(it) } +
        userfieldColumns.map { TableColumn.OfUserfield(it.name) }

/** The grouping the list has: the chosen one, none while its column is gone. */
internal fun effectiveBatteryGrouping(
    options: BatteriesOverviewOptions,
    available: List<TableColumn<BatteriesOverviewGrouping>>,
): TableColumn<BatteriesOverviewGrouping> =
    options.groupingColumn.takeIf { it in available }
        ?: TableColumn.Own(BatteriesOverviewGrouping.NONE)

/** The name and the shown columns, what the list can be sorted by. */
internal fun availableBatterySortColumns(
    fields: Collection<BatteriesOverviewField>,
    userfieldColumns: List<Userfield>,
): List<TableColumn<BatteriesOverviewSortField>> =
    BatteriesOverviewSortField.entries.filter {
        it == BatteriesOverviewSortField.NAME || it.field in fields
    }.map { TableColumn.Own(it) } + if (BatteriesOverviewField.USERFIELDS in fields) {
        userfieldColumns.map { TableColumn.OfUserfield(it.name) }
    } else {
        emptyList()
    }

/** The order the list has: the chosen one, Grocy's default while its column is hidden. */
internal fun effectiveBatterySort(
    options: BatteriesOverviewOptions,
    fields: Collection<BatteriesOverviewField>,
    userfieldColumns: List<Userfield> = emptyList(),
): Pair<TableColumn<BatteriesOverviewSortField>, Boolean> {
    val column = options.sortColumn
    if (column in availableBatterySortColumns(fields, userfieldColumns)) {
        return column to options.ascending
    }
    return TableColumn.Own(defaultBatterySort(fields)) to true
}

/** Grocy's default: by next planned charge cycle, the name while that is hidden. */
internal fun defaultBatterySort(
    fields: Collection<BatteriesOverviewField>,
): BatteriesOverviewSortField =
    if (BatteriesOverviewField.NEXT_CHARGE in fields) {
        BatteriesOverviewSortField.NEXT_CHARGE
    } else {
        BatteriesOverviewSortField.NAME
    }

private val BatteriesOverviewSortField.field: BatteriesOverviewField?
    get() = when (this) {
        BatteriesOverviewSortField.NAME -> null
        BatteriesOverviewSortField.USED_IN -> BatteriesOverviewField.USED_IN
        BatteriesOverviewSortField.LAST_CHARGED -> BatteriesOverviewField.LAST_CHARGED
        BatteriesOverviewSortField.NEXT_CHARGE -> BatteriesOverviewField.NEXT_CHARGE
    }

/**
 * Grocy's batteries overview: the rows filtered by [status] and searched by [query], sorted and
 * grouped. Grocy's status "due soon" includes what is due today. [now] is Grocy's "YYYY-MM-DD
 * HH:MM:SS". Rows of batteries the app does not know yet wait for the next download.
 */
internal fun buildBatteriesOverview(
    data: BatteriesOverviewData,
    status: BatteryDueStatus?,
    sort: Pair<TableColumn<BatteriesOverviewSortField>, Boolean>,
    grouping: TableColumn<BatteriesOverviewGrouping>,
    query: String,
    now: String,
    dueSoonDays: Int,
): List<BatteryGroup> {
    val batteries = data.batteries.associateBy { it.id }
    val columns = data.userfields.filter { it.showAsColumnInTables }
    val sortable = sortableUserfieldColumns(data.userfields)
    val rows = data.current.mapNotNull { current ->
        val battery = batteries[current.batteryId] ?: return@mapNotNull null
        BatteryRow(
            current = current,
            battery = battery,
            dueStatus = batteryDueStatusOf(current.nextEstimatedChargeTime, now, dueSoonDays),
            userfields = columns.mapNotNull { field ->
                userfieldText(field, battery.userfields[field.name])?.let { field.caption to it }
            },
            userfieldTexts = columns.mapNotNull { field ->
                userfieldText(field, battery.userfields[field.name])?.let { field.name to it }
            }.toMap(),
        )
    }
    val filtered = rows.filter { row ->
        status == null || row.dueStatus == status ||
            (status == BatteryDueStatus.DUE_SOON && row.dueStatus == BatteryDueStatus.DUE_TODAY)
    }
    val sorted = sorted(search(filtered, query), sort, sortable)
    val value: (BatteryRow) -> String?
    val order: Comparator<String>
    when (grouping) {
        is TableColumn.OfUserfield -> {
            value = { row -> row.userfieldTexts[grouping.name] }
            order = userfieldTextOrder(sortable.firstOrNull { it.name == grouping.name })
        }
        is TableColumn.Own -> when (grouping.column) {
            BatteriesOverviewGrouping.NONE ->
                return if (sorted.isEmpty()) emptyList() else listOf(BatteryGroup(null, sorted))
            BatteriesOverviewGrouping.USED_IN -> {
                value = { row -> row.battery.usedIn?.ifBlank { null } }
                order = userfieldTextOrder(null)
            }
        }
    }
    val (withValue, withoutValue) = sorted.partition { value(it) != null }
    return withValue.groupBy { value(it).orEmpty() }.entries
        .sortedWith(compareBy(order) { it.key })
        .map { (title, group) -> BatteryGroup(title, group) } +
        listOfNotNull(
            withoutValue.takeIf { it.isNotEmpty() }
                ?.let { BatteryGroup(null, it, isUnassigned = true) },
        )
}

private fun search(rows: List<BatteryRow>, query: String): List<BatteryRow> {
    if (query.isBlank()) return rows
    val matches = FuzzySearch.filter(rows, query) { row ->
        listOfNotNull(row.battery.name, row.battery.usedIn) + row.userfields.map { it.second }
    }.map { it.battery.id }.toSet()
    return rows.filter { it.battery.id in matches }
}

/** Sorted by the chosen column, rows without a value last in both directions, then by name. */
private fun sorted(
    rows: List<BatteryRow>,
    sort: Pair<TableColumn<BatteriesOverviewSortField>, Boolean>,
    userfieldColumns: List<Userfield>,
): List<BatteryRow> {
    val (column, ascending) = sort
    val byName = compareBy<BatteryRow> { it.battery.name.lowercase() }
    val textOrder = if (column is TableColumn.OfUserfield) {
        userfieldTextOrder(userfieldColumns.firstOrNull { it.name == column.name })
    } else {
        naturalOrder()
    }
    val value: (BatteryRow) -> String? = when (column) {
        is TableColumn.OfUserfield -> { row -> row.userfieldTexts[column.name] }
        is TableColumn.Own -> when (column.column) {
            BatteriesOverviewSortField.NAME ->
                return rows.sortedWith(if (ascending) byName else byName.reversed())
            BatteriesOverviewSortField.USED_IN ->
                { row -> row.battery.usedIn?.ifBlank { null }?.lowercase() }
            BatteriesOverviewSortField.LAST_CHARGED -> { row -> row.current.lastTrackedTime }
            BatteriesOverviewSortField.NEXT_CHARGE ->
                { row -> row.current.nextEstimatedChargeTime }
        }
    }
    val (withValue, withoutValue) = rows.partition { value(it) != null }
    val byValue = compareBy<BatteryRow, String>(textOrder) { value(it).orEmpty() }
    val order = if (ascending) byValue else byValue.reversed()
    return withValue.sortedWith(order.then(byName)) + withoutValue.sortedWith(byName)
}
