package com.patrickzedler.grocy.core.model.shoppinglist

/** One of Grocy's shopping lists; Grocy's first list has the id [DEFAULT_ID]. */
data class ShoppingList(
    val id: Int,
    val name: String,
) {
    companion object {
        const val DEFAULT_ID = 1
    }
}
