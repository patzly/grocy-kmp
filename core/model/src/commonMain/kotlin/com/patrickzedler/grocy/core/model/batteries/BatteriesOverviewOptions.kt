package com.patrickzedler.grocy.core.model.batteries

import com.patrickzedler.grocy.core.model.masterdata.TableColumn

/** The columns of Grocy's batteries overview besides the battery, in its order. */
enum class BatteriesOverviewField {
    USED_IN,
    LAST_CHARGED,
    NEXT_CHARGE,

    /** The batteries' userfields Grocy shows as table columns. */
    USERFIELDS,
}

/** The columns Grocy's batteries overview sorts by. */
enum class BatteriesOverviewSortField { NAME, USED_IN, LAST_CHARGED, NEXT_CHARGE }

/** The columns Grocy's "Table options" can group the batteries overview by. */
enum class BatteriesOverviewGrouping { NONE, USED_IN }

/**
 * How the user set up the batteries overview, kept across app starts like Grocy keeps its table
 * options. The defaults are Grocy's: every column shown, not grouped, by next charge cycle.
 */
data class BatteriesOverviewOptions(
    val fields: Set<BatteriesOverviewField> = BatteriesOverviewField.entries.toSet(),
    val grouping: BatteriesOverviewGrouping = BatteriesOverviewGrouping.NONE,
    val sortField: BatteriesOverviewSortField = BatteriesOverviewSortField.NEXT_CHARGE,
    val ascending: Boolean = true,
    /** The userfield column it is sorted by instead of [sortField]. */
    val sortUserfield: String? = null,
    /** The userfield column it is grouped by instead of [grouping]. */
    val groupingUserfield: String? = null,
) {
    val sortColumn: TableColumn<BatteriesOverviewSortField>
        get() = sortUserfield?.let { TableColumn.OfUserfield(it) } ?: TableColumn.Own(sortField)

    val groupingColumn: TableColumn<BatteriesOverviewGrouping>
        get() = groupingUserfield?.let { TableColumn.OfUserfield(it) } ?: TableColumn.Own(grouping)

    fun withSort(
        column: TableColumn<BatteriesOverviewSortField>,
        ascending: Boolean,
    ): BatteriesOverviewOptions = when (column) {
        is TableColumn.Own ->
            copy(sortField = column.column, sortUserfield = null, ascending = ascending)
        is TableColumn.OfUserfield -> copy(sortUserfield = column.name, ascending = ascending)
    }

    fun withGrouping(column: TableColumn<BatteriesOverviewGrouping>): BatteriesOverviewOptions =
        when (column) {
            is TableColumn.Own -> copy(grouping = column.column, groupingUserfield = null)
            is TableColumn.OfUserfield -> copy(groupingUserfield = column.name)
        }

    /** The options as stored, read back by [of]. */
    fun stored(): String = listOf(
        fields.sortedBy { it.ordinal }.joinToString(FIELD_SEPARATOR) { it.name },
        grouping.name,
        sortField.name,
        ascending.toString(),
        sortUserfield.orEmpty(),
        groupingUserfield.orEmpty(),
    ).joinToString(PART_SEPARATOR)

    companion object {
        /** Options from their stored form; missing or unknown values fall back to defaults. */
        fun of(stored: String?): BatteriesOverviewOptions {
            val default = BatteriesOverviewOptions()
            val parts = stored?.split(PART_SEPARATOR) ?: return default
            return BatteriesOverviewOptions(
                fields = parts.getOrNull(0)?.let { names ->
                    names.split(FIELD_SEPARATOR).mapNotNull { name ->
                        BatteriesOverviewField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = BatteriesOverviewGrouping.entries
                    .firstOrNull { it.name == parts.getOrNull(1) } ?: default.grouping,
                sortField = BatteriesOverviewSortField.entries
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
