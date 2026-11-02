package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoreExecution
import com.patrickzedler.grocy.core.model.chores.ChoresJournalField
import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalSortField
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.monthsBefore

/** Grocy's date ranges of the chores journal in months, the last one for everything. */
internal val ChoresJournalRanges = listOf(1, 6, 12, 24, CHORES_JOURNAL_ALL_MONTHS)

internal const val CHORES_JOURNAL_DEFAULT_MONTHS = 12
internal const val CHORES_JOURNAL_ALL_MONTHS = 9999

/** The first day of the range of [months] before [today] like Grocy's, null for everything. */
internal fun choresJournalSince(today: Long, months: Int): String? =
    if (months >= CHORES_JOURNAL_ALL_MONTHS) null else monthsBefore(today, months)

/** Grocy's filters of the chores journal. */
internal data class ChoresJournalFilter(
    val choreId: Int? = null,
    val months: Int = CHORES_JOURNAL_DEFAULT_MONTHS,
) {
    val isActive: Boolean
        get() = choreId != null || months != CHORES_JOURNAL_DEFAULT_MONTHS
}

/** One row of Grocy's chores journal. */
internal data class ChoresJournalItem(
    val execution: ChoreExecution,
    val choreName: String,
    val trackDateOnly: Boolean,
    val userName: String?,
    /** The userfields Grocy shows as table columns with a value, as caption and text. */
    val userfields: List<Pair<String, String>>,
)

/** Rows below a group title; [title] is null without grouping and for an unknown user. */
internal data class ChoresJournalGroup(
    val title: String?,
    val rows: List<ChoresJournalItem>,
    val isUnknownUser: Boolean = false,
)

/** The columns [config]'s features and the server's userfields [hasUserfieldColumns] offer. */
internal fun availableChoresJournalFields(
    config: ServerConfig,
    hasUserfieldColumns: Boolean,
): List<ChoresJournalField> = ChoresJournalField.entries.filter {
    when (it) {
        ChoresJournalField.DONE_BY -> config.choresAssignments
        ChoresJournalField.USERFIELDS -> hasUserfieldColumns
        else -> true
    }
}

/** The groupings [config]'s features offer. */
internal fun availableChoresJournalGroupings(config: ServerConfig): List<ChoresJournalGrouping> =
    ChoresJournalGrouping.entries.filter {
        it != ChoresJournalGrouping.DONE_BY || config.choresAssignments
    }

/** The chore and the shown columns, what the journal can be sorted by. */
internal fun availableChoresJournalSortFields(
    fields: Collection<ChoresJournalField>,
): List<ChoresJournalSortField> = ChoresJournalSortField.entries.filter {
    it == ChoresJournalSortField.CHORE || it.field in fields
}

/** The order the journal has: the chosen one, the default while its column is hidden. */
internal fun effectiveChoresJournalSort(
    options: ChoresJournalOptions,
    fields: Collection<ChoresJournalField>,
): Pair<ChoresJournalSortField, Boolean> {
    if (options.sortField in availableChoresJournalSortFields(fields)) {
        return options.sortField to options.ascending
    }
    return defaultChoresJournalSort(fields)
}

/** Grocy's default: newest tracked time first, by chore while that column is hidden. */
internal fun defaultChoresJournalSort(
    fields: Collection<ChoresJournalField>,
): Pair<ChoresJournalSortField, Boolean> =
    if (ChoresJournalField.TRACKED_TIME in fields) {
        ChoresJournalSortField.TRACKED_TIME to false
    } else {
        ChoresJournalSortField.CHORE to true
    }

private val ChoresJournalSortField.field: ChoresJournalField?
    get() = when (this) {
        ChoresJournalSortField.CHORE -> null
        ChoresJournalSortField.TRACKED_TIME -> ChoresJournalField.TRACKED_TIME
        ChoresJournalSortField.SCHEDULED_TRACKING_TIME ->
            ChoresJournalField.SCHEDULED_TRACKING_TIME
        ChoresJournalSortField.TIME_OF_TRACKING -> ChoresJournalField.TIME_OF_TRACKING
        ChoresJournalSortField.DONE_BY -> ChoresJournalField.DONE_BY
    }

/** The rows of Grocy's chores journal, filtered and searched, sorted and grouped. */
internal fun buildChoresJournal(
    executions: List<ChoreExecution>,
    chores: List<Chore>,
    users: List<User>,
    userfields: List<Userfield>,
    filter: ChoresJournalFilter,
    query: String,
    sort: Pair<ChoresJournalSortField, Boolean> = ChoresJournalSortField.TRACKED_TIME to false,
    grouping: ChoresJournalGrouping = ChoresJournalGrouping.NONE,
): List<ChoresJournalGroup> {
    val choresById = chores.associateBy { it.id }
    val usersById = users.associateBy { it.id }
    val columns = userfields.filter { it.showAsColumnInTables }
    val items = executions
        .filter { filter.choreId == null || it.choreId == filter.choreId }
        .mapNotNull { execution ->
            // Executions of deleted chores have no name to show, like Grocy's table
            val chore = choresById[execution.choreId] ?: return@mapNotNull null
            ChoresJournalItem(
                execution = execution,
                choreName = chore.name,
                trackDateOnly = chore.trackDateOnly,
                userName = execution.doneByUserId?.let { usersById[it]?.displayName },
                userfields = columns.mapNotNull { field ->
                    userfieldText(field, execution.userfields[field.name])
                        ?.let { field.caption to it }
                },
            )
        }
    val rows = sorted(search(items, query), sort)
    return when (grouping) {
        ChoresJournalGrouping.NONE ->
            if (rows.isEmpty()) emptyList() else listOf(ChoresJournalGroup(null, rows))
        ChoresJournalGrouping.CHORE -> titledGroups(rows) { it.choreName }
        ChoresJournalGrouping.DONE_BY -> {
            val (known, unknown) = rows.partition { it.userName != null }
            titledGroups(known) { it.userName.orEmpty() } + listOfNotNull(
                unknown.takeIf { it.isNotEmpty() }
                    ?.let { ChoresJournalGroup(null, it, isUnknownUser = true) },
            )
        }
    }
}

/** One group per title like Grocy's, whose rows group by their cell's text, in its order. */
private fun titledGroups(
    rows: List<ChoresJournalItem>,
    title: (ChoresJournalItem) -> String,
): List<ChoresJournalGroup> = rows.groupBy(title).entries
    .sortedBy { it.key.lowercase() }
    .map { (name, group) -> ChoresJournalGroup(name, group) }

private fun search(items: List<ChoresJournalItem>, query: String): List<ChoresJournalItem> {
    if (query.isBlank()) return items
    val matches = FuzzySearch.filter(items, query) { item ->
        listOfNotNull(item.choreName, item.userName) + item.userfields.map { it.second }
    }.map { it.execution.id }.toSet()
    return items.filter { it.execution.id in matches }
}

/** Sorted by the chosen column, rows without a value last in both directions. */
private fun sorted(
    items: List<ChoresJournalItem>,
    sort: Pair<ChoresJournalSortField, Boolean>,
): List<ChoresJournalItem> {
    val (field, ascending) = sort
    val value: (ChoresJournalItem) -> String? = when (field) {
        ChoresJournalSortField.CHORE -> { item -> item.choreName.lowercase() }
        ChoresJournalSortField.TRACKED_TIME -> { item -> item.execution.trackedTime }
        ChoresJournalSortField.SCHEDULED_TRACKING_TIME ->
            { item -> item.execution.scheduledExecutionTime }
        ChoresJournalSortField.TIME_OF_TRACKING -> { item -> item.execution.createdTimestamp }
        ChoresJournalSortField.DONE_BY -> { item -> item.userName?.lowercase() }
    }
    val (withValue, withoutValue) = items.partition { value(it) != null }
    val byValue = compareBy<ChoresJournalItem> { value(it) }
    return withValue.sortedWith(if (ascending) byValue else byValue.reversed()) + withoutValue
}
