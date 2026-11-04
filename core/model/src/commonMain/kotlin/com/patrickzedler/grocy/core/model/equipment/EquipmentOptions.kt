package com.patrickzedler.grocy.core.model.equipment

import com.patrickzedler.grocy.core.model.masterdata.TableColumn

/** The columns of Grocy's equipment table besides the name. */
enum class EquipmentField {
    /** The userfields Grocy shows as table columns. */
    USERFIELDS,
}

/** The column of its own Grocy's equipment table sorts by. */
enum class EquipmentSortField { NAME }

/** Grocy's equipment only groups by userfield columns. */
enum class EquipmentGrouping { NONE }

/**
 * How the user set up the equipment list, kept across app starts like Grocy keeps its table
 * options. The defaults are Grocy's: every column shown, not grouped, by name.
 */
data class EquipmentOptions(
    val fields: Set<EquipmentField> = EquipmentField.entries.toSet(),
    val ascending: Boolean = true,
    /** The userfield column it is sorted by instead of the name. */
    val sortUserfield: String? = null,
    /** The userfield column it is grouped by. */
    val groupingUserfield: String? = null,
) {
    val sortColumn: TableColumn<EquipmentSortField>
        get() = sortUserfield?.let { TableColumn.OfUserfield(it) }
            ?: TableColumn.Own(EquipmentSortField.NAME)

    val groupingColumn: TableColumn<EquipmentGrouping>
        get() = groupingUserfield?.let { TableColumn.OfUserfield(it) }
            ?: TableColumn.Own(EquipmentGrouping.NONE)

    fun withSort(column: TableColumn<EquipmentSortField>, ascending: Boolean): EquipmentOptions =
        copy(sortUserfield = (column as? TableColumn.OfUserfield)?.name, ascending = ascending)

    fun withGrouping(column: TableColumn<EquipmentGrouping>): EquipmentOptions =
        copy(groupingUserfield = (column as? TableColumn.OfUserfield)?.name)

    /** The options as stored, read back by [of]. */
    fun stored(): String = listOf(
        fields.sortedBy { it.ordinal }.joinToString(FIELD_SEPARATOR) { it.name },
        ascending.toString(),
        sortUserfield.orEmpty(),
        groupingUserfield.orEmpty(),
    ).joinToString(PART_SEPARATOR)

    companion object {
        /** Options from their stored form; missing or unknown values fall back to defaults. */
        fun of(stored: String?): EquipmentOptions {
            val default = EquipmentOptions()
            val parts = stored?.split(PART_SEPARATOR) ?: return default
            return EquipmentOptions(
                fields = parts.getOrNull(0)?.let { names ->
                    names.split(FIELD_SEPARATOR).mapNotNull { name ->
                        EquipmentField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                ascending = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: default.ascending,
                sortUserfield = parts.getOrNull(2)?.ifEmpty { null },
                groupingUserfield = parts.getOrNull(3)?.ifEmpty { null },
            )
        }

        private const val FIELD_SEPARATOR = ","
        private const val PART_SEPARATOR = ";"
    }
}
