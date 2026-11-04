package com.patrickzedler.grocy.apps.shared

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.patrickzedler.grocy.core.data.notifications.Notifier
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary

/** One channel per feature, so each can be set up or turned off in the system's settings too. */
internal class AndroidNotifier(private val context: Context) : Notifier {

    private val manager = NotificationManagerCompat.from(context)

    override suspend fun show(summary: NotificationSummary, silently: Boolean) {
        // Without the permission the system would drop it; a call must not fail because of that
        if (!manager.areNotificationsEnabled()) return
        createChannel(summary.kind)
        val text = notificationTextOf(summary)
        val notification = NotificationCompat.Builder(context, summary.kind.channelId)
            .setSmallIcon(R.drawable.ic_notification)
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
        try {
            manager.notify(summary.kind.notificationId, notification)
        } catch (_: SecurityException) {
            // The permission was revoked just now; the settings page tells the user
        }
    }

    override suspend fun cancel(kind: NotificationKind) {
        manager.cancel(kind.notificationId)
    }

    override suspend fun isShowing(kind: NotificationKind): Boolean =
        manager.activeNotifications.any { it.id == kind.notificationId }

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
