package com.patrickzedler.grocy.apps.shared

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.graphics.toArgb
import com.patrickzedler.grocy.core.data.notifications.Notifier
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.design.appPrimaryColor
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.apps.shared.widget.WIDGET_LINK_EXTRA
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChangeNames
import kotlinx.coroutines.flow.first

/** One channel per feature, so each can be set up or turned off in the system's settings too. */
internal class AndroidNotifier(
    private val context: Context,
    private val settingsStore: SettingsStore,
) : Notifier {

    private val manager = NotificationManagerCompat.from(context)

    override suspend fun show(summary: NotificationSummary, silently: Boolean): Boolean {
        // Without the permission the system would drop it; a call must not fail because of that
        if (!manager.areNotificationsEnabled()) return false
        createChannel(summary.kind)
        val text = notificationTextOf(summary)
        val notification = NotificationCompat.Builder(context, summary.kind.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(appPrimaryColor(context, settingsStore.appearance.first()).toArgb())
            .setContentTitle(text.title)
            .setContentText(text.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.details))
            .setSubText(text.note)
            .setContentIntent(openIntent(summary.kind))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSilent(silently)
            .build()
        return try {
            manager.notify(summary.kind.notificationId, notification)
            true
        } catch (_: SecurityException) {
            // The permission was revoked just now; the settings page tells the user
            false
        }
    }

    override suspend fun cancel(kind: NotificationKind) {
        manager.cancel(kind.notificationId)
    }

    override suspend fun isShowing(kind: NotificationKind): Boolean =
        manager.activeNotifications.any { it.id == kind.notificationId }

    override suspend fun showShoppingListChanges(names: ShoppingListChangeNames): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        val title = shoppingListNotificationTitle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    SHOPPING_CHANNEL_ID,
                    title,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val lines = shoppingListNotificationLines(names)
        val notification = NotificationCompat.Builder(context, SHOPPING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(appPrimaryColor(context, settingsStore.appearance.first()).toArgb())
            .setContentTitle(title)
            .setContentText(lines.joinToString(" "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(shoppingListIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        return try {
            manager.notify(SHOPPING_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override suspend fun cancelShoppingListChanges() {
        manager.cancel(SHOPPING_NOTIFICATION_ID)
    }

    override suspend fun isShowingShoppingListChanges(): Boolean =
        manager.activeNotifications.any { it.id == SHOPPING_NOTIFICATION_ID }

    private fun shoppingListIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        SHOPPING_NOTIFICATION_ID,
        Intent(context, MainActivity::class.java)
            .putExtra(WIDGET_LINK_EXTRA, WidgetLinks.shoppingList())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // Created anew each time, so the name follows the language of the app
    private suspend fun createChannel(kind: NotificationKind) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                kind.channelId,
                notificationTitleOf(kind),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun openIntent(kind: NotificationKind): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_KIND, kind.name)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            kind.notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        private const val SHOPPING_CHANNEL_ID = "shopping_list_changes"

        // Beyond the ids of the kinds, one per enum entry
        private const val SHOPPING_NOTIFICATION_ID = 1000
        private const val EXTRA_KIND = "com.patrickzedler.grocy.NOTIFICATION_KIND"

        /** The kind of the notification that opened [intent], null for every other start. */
        fun openedKindOf(intent: Intent): NotificationKind? {
            val name = intent.getStringExtra(EXTRA_KIND) ?: return null
            // A later recreation of the activity must not open it again
            intent.removeExtra(EXTRA_KIND)
            return NotificationKind.entries.firstOrNull { it.name == name }
        }
    }
}

private val NotificationKind.channelId: String
    get() = "daily_${name.lowercase()}"

// Ids of this app only; 0 is not allowed for some uses of notifications
private val NotificationKind.notificationId: Int
    get() = ordinal + 1
