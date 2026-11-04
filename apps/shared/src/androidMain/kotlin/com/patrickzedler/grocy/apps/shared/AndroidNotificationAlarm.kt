package com.patrickzedler.grocy.apps.shared

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.patrickzedler.grocy.core.data.notifications.NotificationAlarm
import java.util.Calendar
import java.util.TimeZone

/**
 * One alarm for all kinds, at the next time any of them needs. It fires also in Doze. Exact alarms
 * need the user's permission from Android 14 on, the notification settings ask for it; without
 * it Android may deliver it somewhat later.
 */
internal class AndroidNotificationAlarm(private val context: Context) : NotificationAlarm {

    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun set(localMillis: Long) {
        val time = utcMillisOfLocal(localMillis, TimeZone.getDefault())
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()
        try {
            if (canBeExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation())
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation())
            }
        } catch (_: SecurityException) {
            // The user took the permission back just now
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation())
        }
    }

    override fun cancel() {
        manager.cancel(operation())
    }

    private fun operation() = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotificationAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/**
 * The instant of the local time [localMillis] in [zone], by its calendar fields: an offset taken
 * from now would be an hour off for a time beyond a change to or from daylight saving time.
 */
internal fun utcMillisOfLocal(localMillis: Long, zone: TimeZone): Long {
    val local = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = localMillis
    }
    return Calendar.getInstance(zone).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            local.get(Calendar.HOUR_OF_DAY),
            local.get(Calendar.MINUTE),
            local.get(Calendar.SECOND),
        )
    }.timeInMillis
}

/**
 * Runs the check of what is due: when the alarm fires and whenever alarms got lost or wrong, after
 * a restart, an update of the app, a changed time or zone, and with the exact alarms allowed.
 */
class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // The texts' language before Android 13: no activity set it in a process just started
        AndroidAppLanguageStore.wrap(context)
        val graph = (context.applicationContext as? AppGraphOwner)?.graph ?: return
        // The check downloads the current state, the process must live until that is done
        val pendingResult = goAsync()
        graph.checkNotifications(pendingResult::finish)
    }
}
