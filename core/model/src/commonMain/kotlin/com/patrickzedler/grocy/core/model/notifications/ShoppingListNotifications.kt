package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import kotlinx.serialization.Serializable

/** What others do on a shopping list that can notify the user. */
enum class ShoppingListNotificationType { ADDED, CHECKED_OFF }

/** Which changes of the shopping lists notify; off until the user wants them. */
data class ShoppingListNotificationSettings(
    val enabled: Set<ShoppingListNotificationType> = emptySet(),
) {
    val isEnabled: Boolean
        get() = enabled.isNotEmpty()

    operator fun contains(type: ShoppingListNotificationType): Boolean = type in enabled

    fun with(type: ShoppingListNotificationType, isOn: Boolean) = copy(
        enabled = if (isOn) enabled + type else enabled - type,
    )

    fun stored(): String = enabled.joinToString(",") { it.name }

    /** Whether [config] has shopping lists, the same rule as the start screen's section. */
    fun appliesTo(config: ServerConfig): Boolean = isEnabled && config.stock && config.shoppingList

    companion object {
        fun of(stored: String?) = ShoppingListNotificationSettings(
            stored.orEmpty().split(",").mapNotNull { name ->
                ShoppingListNotificationType.entries.firstOrNull { it.name == name }
            }.toSet(),
        )
    }
}

/** What a download brought that this device did not do itself. */
data class ShoppingListChanges(
    val added: List<ShoppingListItem> = emptyList(),
    val checkedOff: List<ShoppingListItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() = added.isEmpty() && checkedOff.isEmpty()
}

/**
 * What [downloaded] has that [stored] had not: this device stores its own changes right away, so
 * what differs was done elsewhere. Items in [pendingDone] are still the user's own to send.
 */
fun shoppingListChangesOf(
    stored: List<ShoppingListItem>,
    downloaded: List<ShoppingListItem>,
    pendingDone: Set<Int>,
): ShoppingListChanges {
    val before = stored.associateBy { it.id }
    return ShoppingListChanges(
        added = downloaded.filter { it.id !in before && !it.done },
        checkedOff = downloaded.filter { item ->
            item.done && item.id !in pendingDone && before[item.id]?.done == false
        },
    )
}

/** The names a notification tells about, kept to grow while it is still showing. */
@Serializable
data class ShoppingListChangeNames(
    val added: List<String> = emptyList(),
    val checkedOff: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = added.isEmpty() && checkedOff.isEmpty()

    operator fun plus(other: ShoppingListChangeNames) = ShoppingListChangeNames(
        added = (added + other.added).distinct(),
        checkedOff = (checkedOff + other.checkedOff).distinct(),
    )
}
