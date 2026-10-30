package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class ShoppingListDto(
    val id: Int,
    val name: FlexString = null,
    val description: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = ShoppingList(
        id = id,
        name = name.orEmpty(),
        description = description?.takeIf { it.isNotBlank() },
        userfields = userfields.orEmpty(),
    )
}

/** Grocy's list form only edits the name, the description is the page's "Notes". */
internal fun ShoppingList.toJson(): JsonObject = buildJsonObject { put("name", name) }

/**
 * An item of Grocy's `shopping_list`. The OpenAPI spec leaves out `done` and `qu_id`, which the
 * table has and the API returns.
 */
@Serializable
internal data class ShoppingListItemDto(
    val id: Int,
    @SerialName("shopping_list_id") val shoppingListId: FlexInt = null,
    @SerialName("product_id") val productId: FlexInt = null,
    val note: FlexString = null,
    val amount: FlexDouble = null,
    @SerialName("qu_id") val quId: FlexInt = null,
    val done: FlexBoolean = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = ShoppingListItem(
        id = id,
        // Grocy's column default
        listId = shoppingListId ?: ShoppingList.DEFAULT_ID,
        productId = productId,
        note = note?.takeIf { it.isNotBlank() },
        amount = amount ?: 0.0,
        quId = quId,
        done = done ?: false,
        userfields = userfields.orEmpty(),
    )
}

/**
 * The fields of Grocy's item form; `done` stays untouched. Without product or unit Grocy gets
 * null, which its triggers leave alone or fill with the product's purchase unit.
 */
internal fun ShoppingListItem.toJson(): JsonObject = buildJsonObject {
    put("shopping_list_id", listId)
    put("product_id", productId)
    put("amount", amount)
    put("qu_id", quId)
    put("note", note?.takeIf { it.isNotBlank() })
}
