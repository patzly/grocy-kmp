package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.core.forms.userfieldText
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

/** The groupings [config]'s features offer. */
internal fun availableChoreGroupings(config: ServerConfig): List<ChoresOverviewGrouping> =
    ChoresOverviewGrouping.entries.filter {
        it != ChoresOverviewGrouping.ASSIGNED_TO || config.choresAssignments
    }

/** The name and the shown columns, what the list can be sorted by. */
internal fun availableChoreSortFields(
    fields: Collection<ChoresOverviewField>,
): List<ChoresOverviewSortField> = ChoresOverviewSortField.entries.filter {
    it == ChoresOverviewSortField.NAME || it.field in fields
}

/** The order the list has: the chosen one, Grocy's default while its column is hidden. */
internal fun effectiveChoreSort(
    options: ChoresOverviewOptions,
    fields: Collection<ChoresOverviewField>,
): Pair<ChoresOverviewSortField, Boolean> {
    val available = availableChoreSortFields(fields)
    if (options.sortField in available) return options.sortField to options.ascending
    return defaultChoreSort(fields) to true
}

/** Grocy's default: by next estimated tracking, the name while that is hidden. */
internal fun defaultChoreSort(fields: Collection<ChoresOverviewField>): ChoresOverviewSortField =
    ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING
        .takeIf { it in availableChoreSortFields(fields) }
        ?: ChoresOverviewSortField.NAME

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
    sort: Pair<ChoresOverviewSortField, Boolean>,
    grouping: ChoresOverviewGrouping,
    query: String,
    now: String,
    dueSoonDays: Int,
): List<ChoreGroup> {
    val chores = data.chores.associateBy { it.id }
    val users = data.users.associateBy { it.id }
    val columns = data.userfields.filter { it.showAsColumnInTables }
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
        )
    }
    val filtered = rows.filter { row ->
        val status = filter.status == null || row.dueStatus == filter.status ||
            (filter.status == ChoreDueStatus.DUE_SOON && row.dueStatus == ChoreDueStatus.DUE_TODAY)
        status && (filter.userId == null || row.chore.assignedUserId == filter.userId)
    }
    val sorted = sorted(search(filtered, query), sort)
    if (grouping == ChoresOverviewGrouping.NONE) {
        return if (sorted.isEmpty()) emptyList() else listOf(ChoreGroup(null, sorted))
    }
    val (assigned, unassigned) = sorted.partition { it.assignedUserName != null }
    return assigned.groupBy { it.assignedUserName }.entries
        .sortedBy { it.key?.lowercase() }
        .map { (name, group) -> ChoreGroup(name, group) } +
        listOfNotNull(
            unassigned.takeIf { it.isNotEmpty() }
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
    sort: Pair<ChoresOverviewSortField, Boolean>,
): List<ChoreRow> {
    val (field, ascending) = sort
    val byName = compareBy<ChoreRow> { it.chore.name.lowercase() }
    if (field == ChoresOverviewSortField.NAME) {
        return rows.sortedWith(if (ascending) byName else byName.reversed())
    }
    val value: (ChoreRow) -> String? = when (field) {
        ChoresOverviewSortField.LAST_TRACKED -> { row -> row.chore.lastTrackedTime }
        ChoresOverviewSortField.ASSIGNED_TO -> { row -> row.assignedUserName?.lowercase() }
        else -> { row -> row.chore.nextEstimatedExecutionTime }
    }
    val (withValue, withoutValue) = rows.partition { value(it) != null }
    val byValue = compareBy<ChoreRow> { value(it) }
    val order = if (ascending) byValue else byValue.reversed()
    return withValue.sortedWith(order.then(byName)) + withoutValue.sortedWith(byName)
}
