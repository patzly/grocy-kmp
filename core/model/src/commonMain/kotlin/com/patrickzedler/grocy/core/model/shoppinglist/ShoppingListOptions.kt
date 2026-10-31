package com.patrickzedler.grocy.core.model.shoppinglist

/** The columns of Grocy's shopping list table besides product and note, in its order. */
enum class ShoppingListField {
    AMOUNT,
    PRODUCT_GROUP,
    LAST_PRICE_UNIT,
    LAST_PRICE_TOTAL,
    DEFAULT_STORE,
    BARCODES,
    /** The item's and its product's userfields Grocy shows as table columns. */
    USERFIELDS,
}

/**
 * Grocy's status filter of the shopping list: its products below their min. stock amount, only
 * done or only undone items.
 */
enum class ShoppingListStatus { BELOW_MIN_STOCK, DONE, UNDONE }

/** The columns Grocy's "Table options" can group the shopping list by. */
enum class ShoppingListGrouping {
    NONE,
    PRODUCT_GROUP,
    DEFAULT_STORE,
}

/**
 * How the user set up the shopping list, kept across app starts like Grocy keeps its table
 * options: the shown fields besides product and note, and the grouping. The defaults are
 * Grocy's: the amount and the userfield columns shown, grouped by product group.
 */
data class ShoppingListOptions(
    val fields: Set<ShoppingListField> = DEFAULT_FIELDS,
    val grouping: ShoppingListGrouping = ShoppingListGrouping.PRODUCT_GROUP,
) {
    companion object {
        val DEFAULT_FIELDS = setOf(ShoppingListField.AMOUNT, ShoppingListField.USERFIELDS)

        /** Options from their stored parts; missing or unknown values fall back to defaults. */
        fun of(fields: String?, grouping: String?): ShoppingListOptions {
            val default = ShoppingListOptions()
            return ShoppingListOptions(
                fields = fields?.let { stored ->
                    stored.split(SEPARATOR).mapNotNull { name ->
                        ShoppingListField.entries.firstOrNull { it.name == name }
                    }.toSet()
                } ?: default.fields,
                grouping = ShoppingListGrouping.entries.firstOrNull { it.name == grouping }
                    ?: default.grouping,
            )
        }

        /** [ShoppingListOptions.fields] as stored, read back by [of]. */
        fun storedFields(fields: Set<ShoppingListField>): String =
            fields.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }

        private const val SEPARATOR = ","
    }
}
