package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.forms.userfieldTextOrder
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewField
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewSortField
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.chores.choreDueStatusOf
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.FuzzySearch

/** Grocy's status and assignment filters of its chores overview. */
internal data class ChoresFilter(
    val status: ChoreDueStatus? = null,
    val userId: Int? = null,
) {
    val isActive: Boolean
        get() = status != null || userId != null
}

/** One row of Grocy's chores overview. */
internal data class ChoreRow(
    val chore: CurrentChore,
    val dueStatus: ChoreDueStatus?,
    val assignedUserName: String?,
    /** Chores without a schedule cannot be skipped. */
    val canSkip: Boolean,
    /** The userfields Grocy shows as table columns with a value, as caption and text. */
    val userfields: List<Pair<String, String>>,
    /** Their texts by the field's name, what sorting and grouping by them use. */
    val userfieldTexts: Map<String, String> = emptyMap(),
)

/** Rows below a group title; [title] is null without grouping and for unassigned rows. */
internal data class ChoreGroup(
    val title: String?,
    val rows: List<ChoreRow>,
    val isUnassigned: Boolean = false,
)

/** What the chores overview's list is built from. */
internal class ChoresOverviewData(
    val current: List<CurrentChore>,
    val chores: List<Chore>,
    val users: List<User>,
    val userfields: List<Userfield>,
)

/** The columns [config]'s features and the server's userfields [hasUserfieldColumns] offer. */
internal fun availableChoreFields(
    config: ServerConfig,
    hasUserfieldColumns: Boolean,
): List<ChoresOverviewField> = ChoresOverviewField.entries.filter {
    when (it) {
        ChoresOverviewField.ASSIGNED_TO -> config.choresAssignments
        ChoresOverviewField.USERFIELDS -> hasUserfieldColumns
        else -> true
    }
}

/** The groupings [config]'s features and the server's [userfieldColumns] offer. */
internal fun availableChoreGroupings(
    config: ServerConfig,
    userfieldColumns: List<Userfield>,
): List<TableColumn<ChoresOverviewGrouping>> =
    ChoresOverviewGrouping.entries.filter {
        it != ChoresOverviewGrouping.ASSIGNED_TO || config.choresAssignments
    }.map { TableColumn.Own(it) } + userfieldColumns.map { TableColumn.OfUserfield(it.name) }

/** The grouping the list has: the chosen one, none while its column is gone. */
internal fun effectiveChoreGrouping(
    options: ChoresOverviewOptions,
    available: List<TableColumn<ChoresOverviewGrouping>>,
): TableColumn<ChoresOverviewGrouping> =
    options.groupingColumn.takeIf { it in available }
        ?: TableColumn.Own(ChoresOverviewGrouping.NONE)

/** The name and the shown columns, what the list can be sorted by. */
internal fun availableChoreSortColumns(
    fields: Collection<ChoresOverviewField>,
    userfieldColumns: List<Userfield>,
): List<TableColumn<ChoresOverviewSortField>> =
    ChoresOverviewSortField.entries.filter {
        it == ChoresOverviewSortField.NAME || it.field in fields
    }.map { TableColumn.Own(it) } + if (ChoresOverviewField.USERFIELDS in fields) {
        userfieldColumns.map { TableColumn.OfUserfield(it.name) }
    } else {
        emptyList()
    }

/** The order the list has: the chosen one, Grocy's default while its column is hidden. */
internal fun effectiveChoreSort(
    options: ChoresOverviewOptions,
    fields: Collection<ChoresOverviewField>,
    userfieldColumns: List<Userfield> = emptyList(),
): Pair<TableColumn<ChoresOverviewSortField>, Boolean> {
    val column = options.sortColumn
    if (column in availableChoreSortColumns(fields, userfieldColumns)) {
        return column to options.ascending
    }
    return TableColumn.Own(defaultChoreSort(fields)) to true
}

/** Grocy's default: by next estimated tracking, the name while that is hidden. */
internal fun defaultChoreSort(fields: Collection<ChoresOverviewField>): ChoresOverviewSortField =
    if (ChoresOverviewField.NEXT_ESTIMATED_TRACKING in fields) {
        ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING
    } else {
        ChoresOverviewSortField.NAME
    }

private val ChoresOverviewSortField.field: ChoresOverviewField?
    get() = when (this) {
        ChoresOverviewSortField.NAME -> null
        ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING ->
            ChoresOverviewField.NEXT_ESTIMATED_TRACKING
        ChoresOverviewSortField.LAST_TRACKED -> ChoresOverviewField.LAST_TRACKED
        ChoresOverviewSortField.ASSIGNED_TO -> ChoresOverviewField.ASSIGNED_TO
    }

/**
 * Grocy's chores overview: the rows [filter]ed and searched by [query], sorted and grouped.
 * Grocy's status "due soon" includes what is due today. [now] is Grocy's "YYYY-MM-DD HH:MM:SS".
 */
internal fun buildChoresOverview(
    data: ChoresOverviewData,
    filter: ChoresFilter,
    sort: Pair<TableColumn<ChoresOverviewSortField>, Boolean>,
    grouping: TableColumn<ChoresOverviewGrouping>,
    query: String,
    now: String,
    dueSoonDays: Int,
): List<ChoreGroup> {
    val chores = data.chores.associateBy { it.id }
    val users = data.users.associateBy { it.id }
    val columns = data.userfields.filter { it.showAsColumnInTables }
    val sortable = sortableUserfieldColumns(data.userfields)
    val rows = data.current.map { chore ->
        val master = chores[chore.choreId]
        ChoreRow(
            chore = chore,
            dueStatus = choreDueStatusOf(chore.nextEstimatedExecutionTime, now, dueSoonDays),
            assignedUserName = chore.assignedUserId?.let { users[it]?.displayName },
            canSkip = master?.periodType != ChorePeriodType.MANUALLY,
            userfields = columns.mapNotNull { field ->
                userfieldText(field, master?.userfields?.get(field.name))
                    ?.let { field.caption to it }
            },
            userfieldTexts = columns.mapNotNull { field ->
                userfieldText(field, master?.userfields?.get(field.name))
                    ?.let { field.name to it }
            }.toMap(),
        )
    }
    val filtered = rows.filter { row ->
        val status = filter.status == null || row.dueStatus == filter.status ||
            (filter.status == ChoreDueStatus.DUE_SOON && row.dueStatus == ChoreDueStatus.DUE_TODAY)
        status && (filter.userId == null || row.chore.assignedUserId == filter.userId)
    }
    val sorted = sorted(search(filtered, query), sort, sortable)
    val value: (ChoreRow) -> String?
    val order: Comparator<String>
    when (grouping) {
        is TableColumn.OfUserfield -> {
            value = { row -> row.userfieldTexts[grouping.name] }
            order = userfieldTextOrder(sortable.firstOrNull { it.name == grouping.name })
        }
        is TableColumn.Own -> when (grouping.column) {
            ChoresOverviewGrouping.NONE ->
                return if (sorted.isEmpty()) emptyList() else listOf(ChoreGroup(null, sorted))
            ChoresOverviewGrouping.ASSIGNED_TO -> {
                value = { row -> row.assignedUserName }
                order = userfieldTextOrder(null)
            }
        }
    }
    val (withValue, withoutValue) = sorted.partition { value(it) != null }
    return withValue.groupBy { value(it).orEmpty() }.entries
        .sortedWith(compareBy(order) { it.key })
        .map { (title, group) -> ChoreGroup(title, group) } +
        listOfNotNull(
            withoutValue.takeIf { it.isNotEmpty() }
                ?.let { ChoreGroup(null, it, isUnassigned = true) },
        )
}

private fun search(rows: List<ChoreRow>, query: String): List<ChoreRow> {
    if (query.isBlank()) return rows
    val matches = FuzzySearch.filter(rows, query) { row ->
        listOfNotNull(row.chore.name, row.assignedUserName) + row.userfields.map { it.second }
    }.map { it.chore.choreId }.toSet()
    return rows.filter { it.chore.choreId in matches }
}

/** Sorted by the chosen column, rows without a value last in both directions, then by name. */
private fun sorted(
    rows: List<ChoreRow>,
    sort: Pair<TableColumn<ChoresOverviewSortField>, Boolean>,
    userfieldColumns: List<Userfield>,
): List<ChoreRow> {
    val (column, ascending) = sort
    val byName = compareBy<ChoreRow> { it.chore.name.lowercase() }
    val textOrder = if (column is TableColumn.OfUserfield) {
        userfieldTextOrder(userfieldColumns.firstOrNull { it.name == column.name })
    } else {
        naturalOrder()
    }
    val value: (ChoreRow) -> String? = when (column) {
        is TableColumn.OfUserfield -> { row -> row.userfieldTexts[column.name] }
        is TableColumn.Own -> when (column.column) {
            ChoresOverviewSortField.NAME ->
                return rows.sortedWith(if (ascending) byName else byName.reversed())
            ChoresOverviewSortField.LAST_TRACKED -> { row -> row.chore.lastTrackedTime }
            ChoresOverviewSortField.ASSIGNED_TO -> { row -> row.assignedUserName?.lowercase() }
            ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING ->
                { row -> row.chore.nextEstimatedExecutionTime }
        }
    }
    val (withValue, withoutValue) = rows.partition { value(it) != null }
    val byValue = compareBy<ChoreRow, String>(textOrder) { value(it).orEmpty() }
    val order = if (ascending) byValue else byValue.reversed()
    return withValue.sortedWith(order.then(byName)) + withoutValue.sortedWith(byName)
}
