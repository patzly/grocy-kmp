package com.patrickzedler.grocy.core.model.chores

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
            )
        }

        private const val FIELD_SEPARATOR = ","
        private const val PART_SEPARATOR = ";"
    }
}
