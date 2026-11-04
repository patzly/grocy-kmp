package com.patrickzedler.grocy.core.model.batteries

/** The columns of Grocy's batteries journal besides the battery, in its order. */
enum class BatteriesJournalField {
    TRACKED_TIME,

    /** The charge cycles' userfields Grocy shows as table columns. */
    USERFIELDS,
}

/** The columns Grocy's batteries journal sorts by. */
enum class BatteriesJournalSortField { BATTERY, TRACKED_TIME }

/** The columns Grocy's "Table options" can group the batteries journal by. */
enum class BatteriesJournalGrouping { NONE, BATTERY }

/**
 * How the user set up the batteries journal, kept across app starts like Grocy keeps its table
 * options. The defaults are Grocy's: every column shown, not grouped, newest tracked time first.
 */
data class BatteriesJournalOptions(
    val fields: Set<BatteriesJournalField> = BatteriesJournalField.entries.toSet(),
    val grouping: BatteriesJournalGrouping = BatteriesJournalGrouping.NONE,
    val sortField: BatteriesJournalSortField = BatteriesJournalSortField.TRACKED_TIME,
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
        fun of(stored: String?): BatteriesJournalOptions {
            val default = BatteriesJournalOptions()
            val parts = stored?.split(PART_SEPARATOR) ?: return default
            return BatteriesJournalOptions(
                fields = parts.getOrNull(0)?.let { names ->
                    names.split(FIELD_SEPARATOR).mapNotNull { name ->
                        BatteriesJournalField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = BatteriesJournalGrouping.entries
                    .firstOrNull { it.name == parts.getOrNull(1) } ?: default.grouping,
                sortField = BatteriesJournalSortField.entries
                    .firstOrNull { it.name == parts.getOrNull(2) } ?: default.sortField,
                ascending = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: default.ascending,
            )
        }

        private const val FIELD_SEPARATOR = ","
        private const val PART_SEPARATOR = ";"
    }
}
