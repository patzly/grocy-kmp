package com.patrickzedler.grocy.feature.equipment.impl

import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.forms.userfieldText
import com.patrickzedler.grocy.core.forms.userfieldTextOrder
import com.patrickzedler.grocy.core.model.equipment.Equipment
import com.patrickzedler.grocy.core.model.equipment.EquipmentField
import com.patrickzedler.grocy.core.model.equipment.EquipmentGrouping
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.equipment.EquipmentSortField
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.text.FuzzySearch

/** One row of Grocy's equipment table. */
internal data class EquipmentRow(
    val equipment: Equipment,
    /** The userfields Grocy shows as table columns with a value, as caption and text. */
    val userfields: List<Pair<String, String>>,
    /** Their texts by the field's name, what sorting and grouping by them use. */
    val userfieldTexts: Map<String, String> = emptyMap(),
)

/** Rows below a group title; [title] is null without grouping and for rows without a value. */
internal data class EquipmentGroup(
    val title: String?,
    val rows: List<EquipmentRow>,
    val isUnassigned: Boolean = false,
)

/** The name and, while shown, the userfield columns, what the list can be sorted by. */
internal fun availableEquipmentSortColumns(
    fields: Collection<EquipmentField>,
    userfieldColumns: List<Userfield>,
): List<TableColumn<EquipmentSortField>> =
    listOf(TableColumn.Own(EquipmentSortField.NAME)) +
        if (EquipmentField.USERFIELDS in fields) {
            userfieldColumns.map { TableColumn.OfUserfield(it.name) }
        } else {
            emptyList()
        }

/** No grouping and the server's [userfieldColumns]. */
internal fun availableEquipmentGroupings(
    userfieldColumns: List<Userfield>,
): List<TableColumn<EquipmentGrouping>> =
    listOf(TableColumn.Own(EquipmentGrouping.NONE)) +
        userfieldColumns.map { TableColumn.OfUserfield(it.name) }

/** The order the list has: the chosen one, by name while its column is gone. */
internal fun effectiveEquipmentSort(
    options: EquipmentOptions,
    fields: Collection<EquipmentField>,
    userfieldColumns: List<Userfield>,
): Pair<TableColumn<EquipmentSortField>, Boolean> {
    val column = options.sortColumn
    return if (column in availableEquipmentSortColumns(fields, userfieldColumns)) {
        column to options.ascending
    } else {
        TableColumn.Own(EquipmentSortField.NAME) to true
    }
}

/** The grouping the list has: the chosen one, none while its column is gone. */
internal fun effectiveEquipmentGrouping(
    options: EquipmentOptions,
    available: List<TableColumn<EquipmentGrouping>>,
): TableColumn<EquipmentGrouping> =
    options.groupingColumn.takeIf { it in available } ?: TableColumn.Own(EquipmentGrouping.NONE)

/** Grocy's equipment table: the rows searched by [query], sorted and grouped. */
internal fun buildEquipmentList(
    equipment: List<Equipment>,
    userfields: List<Userfield>,
    sort: Pair<TableColumn<EquipmentSortField>, Boolean>,
    grouping: TableColumn<EquipmentGrouping>,
    query: String,
): List<EquipmentGroup> {
    val columns = sortableUserfieldColumns(userfields)
    val rows = equipment.map { piece ->
        val texts = columns.mapNotNull { field ->
            userfieldText(field, piece.userfields[field.name])?.let { field to it }
        }
        EquipmentRow(
            equipment = piece,
            userfields = texts.map { (field, text) -> field.caption to text },
            userfieldTexts = texts.associate { (field, text) -> field.name to text },
        )
    }
    val sorted = sorted(search(rows, query), sort, columns)
    val column = grouping as? TableColumn.OfUserfield
        ?: return if (sorted.isEmpty()) emptyList() else listOf(EquipmentGroup(null, sorted))
    val order = userfieldTextOrder(columns.firstOrNull { it.name == column.name })
    val (withValue, withoutValue) = sorted.partition { column.name in it.userfieldTexts }
    return withValue.groupBy { it.userfieldTexts.getValue(column.name) }.entries
        .sortedWith(compareBy(order) { it.key })
        .map { (title, group) -> EquipmentGroup(title, group) } +
        listOfNotNull(
            withoutValue.takeIf { it.isNotEmpty() }
                ?.let { EquipmentGroup(null, it, isUnassigned = true) },
        )
}

private fun search(rows: List<EquipmentRow>, query: String): List<EquipmentRow> {
    if (query.isBlank()) return rows
    val matches = FuzzySearch.filter(rows, query) { row ->
        listOf(row.equipment.name) + row.userfields.map { it.second }
    }.map { it.equipment.id }.toSet()
    return rows.filter { it.equipment.id in matches }
}

/** Sorted by the chosen column, rows without a value last in both directions, then by name. */
private fun sorted(
    rows: List<EquipmentRow>,
    sort: Pair<TableColumn<EquipmentSortField>, Boolean>,
    columns: List<Userfield>,
): List<EquipmentRow> {
    val (column, ascending) = sort
    val byName = compareBy<EquipmentRow> { it.equipment.name.lowercase() }
    if (column !is TableColumn.OfUserfield) {
        return rows.sortedWith(if (ascending) byName else byName.reversed())
    }
    val textOrder = userfieldTextOrder(columns.firstOrNull { it.name == column.name })
    val (withValue, withoutValue) = rows.partition { column.name in it.userfieldTexts }
    val byValue =
        compareBy<EquipmentRow, String>(textOrder) { it.userfieldTexts.getValue(column.name) }
    val order = if (ascending) byValue else byValue.reversed()
    return withValue.sortedWith(order.then(byName)) + withoutValue.sortedWith(byName)
}
