package com.patrickzedler.grocy.core.model.chores

import com.patrickzedler.grocy.core.model.masterdata.TableColumn

/** The columns of Grocy's chores overview besides the chore, in its order. */
enum class ChoresOverviewField {
    NEXT_ESTIMATED_TRACKING,
    LAST_TRACKED,
    ASSIGNED_TO,

    /** The chores' userfields Grocy shows as table columns. */
    USERFIELDS,
}

/** The columns Grocy's chores overview sorts by. */
enum class ChoresOverviewSortField { NAME, NEXT_ESTIMATED_TRACKING, LAST_TRACKED, ASSIGNED_TO }

/** The columns Grocy's "Table options" can group the chores overview by. */
enum class ChoresOverviewGrouping { NONE, ASSIGNED_TO }

/**
 * How the user set up the chores overview, kept across app starts like Grocy keeps its table
 * options. The defaults are Grocy's: every column shown, not grouped, by next estimated tracking.
 */
data class ChoresOverviewOptions(
    val fields: Set<ChoresOverviewField> = ChoresOverviewField.entries.toSet(),
    val grouping: ChoresOverviewGrouping = ChoresOverviewGrouping.NONE,
    val sortField: ChoresOverviewSortField = ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING,
    val ascending: Boolean = true,
    /** The userfield column it is sorted by instead of [sortField]. */
    val sortUserfield: String? = null,
    /** The userfield column it is grouped by instead of [grouping]. */
    val groupingUserfield: String? = null,
) {
    val sortColumn: TableColumn<ChoresOverviewSortField>
        get() = sortUserfield?.let { TableColumn.OfUserfield(it) }
            ?: TableColumn.Own(sortField)

    val groupingColumn: TableColumn<ChoresOverviewGrouping>
        get() = groupingUserfield?.let { TableColumn.OfUserfield(it) }
            ?: TableColumn.Own(grouping)

    fun withSort(
        column: TableColumn<ChoresOverviewSortField>,
        ascending: Boolean,
    ): ChoresOverviewOptions =
        when (column) {
            is TableColumn.Own ->
                copy(sortField = column.column, sortUserfield = null, ascending = ascending)
            is TableColumn.OfUserfield -> copy(sortUserfield = column.name, ascending = ascending)
        }

    fun withGrouping(column: TableColumn<ChoresOverviewGrouping>): ChoresOverviewOptions =
        when (column) {
            is TableColumn.Own -> copy(grouping = column.column, groupingUserfield = null)
            is TableColumn.OfUserfield -> copy(groupingUserfield = column.name)
        }

    /** The options as stored, read back by [of]; userfield columns only when chosen. */
    fun stored(): String = (
        listOf(
            fields.sortedBy { it.ordinal }.joinToString(FIELD_SEPARATOR) { it.name },
            grouping.name,
            sortField.name,
            ascending.toString(),
        ) + if (sortUserfield != null || groupingUserfield != null) {
            listOf(sortUserfield.orEmpty(), groupingUserfield.orEmpty())
        } else {
            emptyList()
        }
        ).joinToString(PART_SEPARATOR)

    companion object {
        /** Options from their stored form; missing or unknown values fall back to defaults. */
        fun of(stored: String?): ChoresOverviewOptions {
            val default = ChoresOverviewOptions()
            val parts = stored?.split(PART_SEPARATOR) ?: return default
            return ChoresOverviewOptions(
                fields = parts.getOrNull(0)?.let { names ->
                    names.split(FIELD_SEPARATOR).mapNotNull { name ->
                        ChoresOverviewField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = ChoresOverviewGrouping.entries
                    .firstOrNull { it.name == parts.getOrNull(1) } ?: default.grouping,
                sortField = ChoresOverviewSortField.entries
                    .firstOrNull { it.name == parts.getOrNull(2) } ?: default.sortField,
                ascending = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: default.ascending,
                sortUserfield = parts.getOrNull(4)?.ifEmpty { null },
                groupingUserfield = parts.getOrNull(5)?.ifEmpty { null },
            )
        }

        private const val FIELD_SEPARATOR = ","
        private const val PART_SEPARATOR = ";"
    }
}
