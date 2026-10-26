package com.patrickzedler.grocy.core.model.stock

/** The columns of Grocy's stock overview table, in its order. */
enum class StockOverviewField {
    NAME,
    PRODUCT_GROUP,
    AMOUNT,
    VALUE,
    NEXT_DUE_DATE,
    CALORIES_PER_UNIT,
    CALORIES,
    LAST_PURCHASED,
    LAST_PRICE,
    MIN_STOCK_AMOUNT,
    DESCRIPTION,
    PARENT_PRODUCT,
    DEFAULT_LOCATION,
    PICTURE,
    AVERAGE_PRICE,
    DEFAULT_STORE,
}

/** The columns Grocy's "Table options" can group the stock overview by. */
enum class StockOverviewGrouping {
    NONE,
    PRODUCT_GROUP,
    NEXT_DUE_DATE,
    LAST_PURCHASED,
    MIN_STOCK_AMOUNT,
    PARENT_PRODUCT,
    DEFAULT_LOCATION,
    DEFAULT_STORE,
}

/**
 * How the user set up the stock overview, kept across app starts like Grocy keeps its table
 * options: the shown fields besides the name (always the item's title), grouping and sort.
 * The defaults are Grocy's: amount and next due date shown, sorted by the next due date.
 */
data class StockOverviewOptions(
    val fields: Set<StockOverviewField> = DEFAULT_FIELDS,
    val grouping: StockOverviewGrouping = StockOverviewGrouping.NONE,
    val sortField: StockOverviewField = StockOverviewField.NEXT_DUE_DATE,
    val ascending: Boolean = true,
) {
    companion object {
        val DEFAULT_FIELDS = setOf(StockOverviewField.AMOUNT, StockOverviewField.NEXT_DUE_DATE)

        /** Options from their stored parts; missing or unknown values fall back to defaults. */
        fun of(
            fields: String?,
            grouping: String?,
            sortField: String?,
            ascending: String?,
        ): StockOverviewOptions {
            val default = StockOverviewOptions()
            return StockOverviewOptions(
                fields = fields?.let { stored ->
                    stored.split(SEPARATOR).mapNotNull { name ->
                        StockOverviewField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = StockOverviewGrouping.entries.firstOrNull { it.name == grouping }
                    ?: default.grouping,
                sortField = StockOverviewField.entries.firstOrNull { it.name == sortField }
                    ?: default.sortField,
                ascending = ascending?.toBooleanStrictOrNull() ?: default.ascending,
            )
        }

        /** [StockOverviewOptions.fields] as stored, read back by [of]. */
        fun storedFields(fields: Set<StockOverviewField>): String =
            fields.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }

        private const val SEPARATOR = ","
    }
}
