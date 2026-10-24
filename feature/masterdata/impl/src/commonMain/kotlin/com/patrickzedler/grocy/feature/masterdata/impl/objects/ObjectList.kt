package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.runtime.Immutable
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.normalizedForSearch

/** The columns of Grocy's master data tables, the fields a list can be sorted by. */
internal enum class ObjectColumn {
    NAME,
    DESCRIPTION,
    PRODUCT_COUNT,
    PERIOD_TYPE,
    USED_IN,
    CHARGE_INTERVAL,
    ENTITY,
    CAPTION,
    TYPE,
    SORT_NUMBER,
}

/** A row of one of Grocy's tables: its column values as the table shows them. */
@Immutable
internal data class ObjectRow(
    val id: Int,
    val active: Boolean = true,
    /** Shown values by column; empty ones are left out. */
    val texts: Map<ObjectColumn, String>,
    /** Numeric columns sort by these values instead of their text. */
    val numbers: Map<ObjectColumn, Double> = emptyMap(),
    /** The userfield entity of a userfield, for the entity filter. */
    val entity: String? = null,
)

/** The filters of Grocy's master data lists; the defaults show every active object. */
@Immutable
internal data class ObjectFilter(
    val showDisabled: Boolean = false,
    /** Only userfields of this entity, null for all. */
    val entity: String? = null,
) {
    val isActive: Boolean
        get() = this != ObjectFilter()
}

@Immutable
internal data class ObjectSort(val column: ObjectColumn, val ascending: Boolean = true)

/**
 * The rows Grocy's table shows for [filter] and [query], sorted by [sort]. Like Grocy: disabled
 * objects only with [ObjectFilter.showDisabled], the search looks at every column and orders by
 * relevance, otherwise ties of the sorted column keep the order of [nameColumn].
 */
internal fun buildObjectList(
    rows: List<ObjectRow>,
    filter: ObjectFilter,
    sort: ObjectSort,
    query: String,
    nameColumn: ObjectColumn = ObjectColumn.NAME,
): List<ObjectRow> {
    val filtered = rows.filter { row ->
        (filter.showDisabled || row.active) &&
            (filter.entity == null || row.entity == filter.entity)
    }
    if (query.isNotBlank()) {
        return FuzzySearch.filter(filtered, query) { row -> row.texts.values.toList() }
    }
    return filtered.sortedWith(comparator(sort, nameColumn))
}

private fun comparator(sort: ObjectSort, nameColumn: ObjectColumn): Comparator<ObjectRow> {
    val byName = compareBy<ObjectRow> { it.texts[nameColumn]?.normalizedForSearch() }
    val column = sort.column
    val values: Comparator<ObjectRow> = compareBy<ObjectRow> { it.numbers[column] }
        .thenBy { it.texts[column]?.normalizedForSearch() }
    val directed = if (sort.ascending) values else values.reversed()
    // Empty values last in both directions, like a table sorted by text puts them together
    val emptyLast = compareBy<ObjectRow> { it.texts[column].isNullOrBlank() }
    return if (column == nameColumn) {
        if (sort.ascending) byName else byName.reversed()
    } else {
        emptyLast.then(directed).then(byName)
    }
}
