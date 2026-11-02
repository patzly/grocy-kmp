package com.patrickzedler.grocy.core.model.chores

/** The columns of Grocy's chores journal besides the chore, in its order. */
enum class ChoresJournalField {
    TRACKED_TIME,
    SCHEDULED_TRACKING_TIME,

    /** When the execution was entered, Grocy's "Time of tracking", hidden by default. */
    TIME_OF_TRACKING,
    DONE_BY,

    /** The executions' userfields Grocy shows as table columns. */
    USERFIELDS,
}

/** The columns Grocy's chores journal sorts by. */
enum class ChoresJournalSortField {
    CHORE,
    TRACKED_TIME,
    SCHEDULED_TRACKING_TIME,
    TIME_OF_TRACKING,
    DONE_BY,
}

/** The columns Grocy's "Table options" can group the chores journal by. */
enum class ChoresJournalGrouping { NONE, CHORE, DONE_BY }

/**
 * How the user set up the chores journal, kept across app starts like Grocy keeps its table
 * options. The defaults are Grocy's: every column but "Time of tracking", not grouped, newest
 * tracked time first.
 */
data class ChoresJournalOptions(
    val fields: Set<ChoresJournalField> =
        ChoresJournalField.entries.toSet() - ChoresJournalField.TIME_OF_TRACKING,
    val grouping: ChoresJournalGrouping = ChoresJournalGrouping.NONE,
    val sortField: ChoresJournalSortField = ChoresJournalSortField.TRACKED_TIME,
    val ascending: Boolean = false,
) {
    /** The options as stored, read back by [of]. */
    fun stored(): String = listOf(
        fields.sortedBy { it.ordinal }.joinToString(FIELD_SEPARATOR) { it.name },
        grouping.name,
        sortField.name,
        ascending.toString(),
    ).joinToString(PART_SEPARATOR)

    companion object {
        /** Options from their stored form; missing or unknown values fall back to defaults. */
        fun of(stored: String?): ChoresJournalOptions {
            val default = ChoresJournalOptions()
            val parts = stored?.split(PART_SEPARATOR) ?: return default
            return ChoresJournalOptions(
                fields = parts.getOrNull(0)?.let { names ->
                    names.split(FIELD_SEPARATOR).mapNotNull { name ->
                        ChoresJournalField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = ChoresJournalGrouping.entries
                    .firstOrNull { it.name == parts.getOrNull(1) } ?: default.grouping,
                sortField = ChoresJournalSortField.entries
                    .firstOrNull { it.name == parts.getOrNull(2) } ?: default.sortField,
                ascending = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: default.ascending,
            )
        }

        private const val FIELD_SEPARATOR = ","
        private const val PART_SEPARATOR = ";"
    }
}
