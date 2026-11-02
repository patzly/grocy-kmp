package com.patrickzedler.grocy.core.model.tasks

import com.patrickzedler.grocy.core.model.masterdata.TableColumn

/** The columns of Grocy's tasks page besides the task, in its order. */
enum class TasksOverviewField {
    DUE,
    CATEGORY,
    ASSIGNED_TO,

    /** The tasks' userfields Grocy shows as table columns. */
    USERFIELDS,
}

/** The columns Grocy's tasks page sorts by. */
enum class TasksOverviewSortField { NAME, DUE, CATEGORY, ASSIGNED_TO }

/** The columns Grocy's "Table options" can group the tasks page by. */
enum class TasksOverviewGrouping { NONE, DUE, CATEGORY, ASSIGNED_TO }

/**
 * How the user set up the tasks page, kept across app starts like Grocy keeps its table options.
 * The defaults are Grocy's: every column shown, not grouped, by due date.
 */
data class TasksOverviewOptions(
    val fields: Set<TasksOverviewField> = TasksOverviewField.entries.toSet(),
    val grouping: TasksOverviewGrouping = TasksOverviewGrouping.NONE,
    val sortField: TasksOverviewSortField = TasksOverviewSortField.DUE,
    val ascending: Boolean = true,
    /** The userfield column it is sorted by instead of [sortField]. */
    val sortUserfield: String? = null,
    /** The userfield column it is grouped by instead of [grouping]. */
    val groupingUserfield: String? = null,
) {
    val sortColumn: TableColumn<TasksOverviewSortField>
        get() = sortUserfield?.let { TableColumn.OfUserfield(it) }
            ?: TableColumn.Own(sortField)

    val groupingColumn: TableColumn<TasksOverviewGrouping>
        get() = groupingUserfield?.let { TableColumn.OfUserfield(it) }
            ?: TableColumn.Own(grouping)

    fun withSort(
        column: TableColumn<TasksOverviewSortField>,
        ascending: Boolean,
    ): TasksOverviewOptions =
        when (column) {
            is TableColumn.Own ->
                copy(sortField = column.column, sortUserfield = null, ascending = ascending)
            is TableColumn.OfUserfield -> copy(sortUserfield = column.name, ascending = ascending)
        }

    fun withGrouping(column: TableColumn<TasksOverviewGrouping>): TasksOverviewOptions =
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
        fun of(stored: String?): TasksOverviewOptions {
            val default = TasksOverviewOptions()
            val parts = stored?.split(PART_SEPARATOR) ?: return default
            return TasksOverviewOptions(
                fields = parts.getOrNull(0)?.let { names ->
                    names.split(FIELD_SEPARATOR).mapNotNull { name ->
                        TasksOverviewField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = TasksOverviewGrouping.entries
                    .firstOrNull { it.name == parts.getOrNull(1) } ?: default.grouping,
                sortField = TasksOverviewSortField.entries
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
