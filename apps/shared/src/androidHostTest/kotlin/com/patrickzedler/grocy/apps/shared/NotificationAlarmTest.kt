package com.patrickzedler.grocy.apps.shared

import java.io.File
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationAlarmTest {

    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    // 2026-03-28 09:00 and 2026-03-29 09:00, the clocks move forward in between
    private val saturday = 1_774_688_400_000L
    private val sunday = saturday + 86_400_000L

    @Test
    fun winterTimeIsOneHourAhead() {
        assertEquals(saturday - 3_600_000L, utcMillisOfLocal(saturday, berlin))
    }

    @Test
    fun aTimeBeyondTheClockChangeUsesItsOwnOffset() {
        assertEquals(sunday - 2 * 3_600_000L, utcMillisOfLocal(sunday, berlin))
    }

    @Test
    fun manifestDeclaresWhatTheAlarmNeeds() {
        val manifest = File("src/androidMain/AndroidManifest.xml").readText()

        listOf(
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED",
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            "NotificationAlarmReceiver",
        ).forEach { assertTrue(it in manifest, "$it is missing") }
    }
}
