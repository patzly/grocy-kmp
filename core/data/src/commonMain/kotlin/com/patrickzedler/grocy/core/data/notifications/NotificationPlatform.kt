package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChangeNames

/** Shows notifications the way the platform does; at most one per kind at a time. */
interface Notifier {
    /**
     * Shows [summary], replacing the one of its kind; [silently] does not alert the user again.
     * Returns false when the platform did not take it, e.g. because notifications are blocked.
     */
    suspend fun show(summary: NotificationSummary, silently: Boolean): Boolean

    suspend fun cancel(kind: NotificationKind)

    /** Whether the one of [kind] is still there, not dismissed by the user. */
    suspend fun isShowing(kind: NotificationKind): Boolean

    /**
     * Tells what others did on the shopping lists, replacing what it told before. Returns false
     * when the platform did not take it.
     */
    suspend fun showShoppingListChanges(names: ShoppingListChangeNames): Boolean

    suspend fun cancelShoppingListChanges()

    /** Whether the one about the shopping lists is still there, not dismissed by the user. */
    suspend fun isShowingShoppingListChanges(): Boolean
}

/** Wakes the app so [NotificationRepository.check] runs at a time. */
interface NotificationAlarm {
    /**
     * Replaces the alarm with one at [localMillis] (local time like `localNowMillis`), which
     * fires at once when that passed already.
     */
    fun set(localMillis: Long)

    fun cancel()
}
