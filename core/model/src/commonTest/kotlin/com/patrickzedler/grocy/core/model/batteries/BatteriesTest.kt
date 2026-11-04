package com.patrickzedler.grocy.core.model.batteries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BatteriesTest {
    private val now = "2026-11-04 12:00:00"

    @Test
    fun statusFollowsGrocysOverview() {
        assertEquals(
            BatteryDueStatus.OVERDUE,
            batteryDueStatusOf("2026-11-04 11:59:59", now, dueSoonDays = 5),
        )
        assertEquals(
            BatteryDueStatus.DUE_TODAY,
            batteryDueStatusOf("2026-11-04 23:59:59", now, dueSoonDays = 5),
        )
        assertEquals(
            BatteryDueStatus.DUE_SOON,
            batteryDueStatusOf("2026-11-09 12:00:00", now, dueSoonDays = 5),
        )
        assertNull(batteryDueStatusOf("2026-11-09 12:00:01", now, dueSoonDays = 5))
    }

    @Test
    fun batteriesWithoutNextChargeHaveNoStatus() {
        assertNull(batteryDueStatusOf(null, now, dueSoonDays = 5))
    }

    @Test
    fun zeroDueSoonDaysLeaveOnlyTodayAndOverdue() {
        assertNull(batteryDueStatusOf("2026-11-05 00:00:00", now, dueSoonDays = 0))
        assertEquals(
            BatteryDueStatus.DUE_TODAY,
            batteryDueStatusOf("2026-11-04 18:00:00", now, dueSoonDays = 0),
        )
    }

    @Test
    fun summaryCountsTodayAlsoAsDueSoonLikeGrocysMessages() {
        val summary = batteriesSummaryOf(
            batteries = listOf(
                CurrentBattery(1, nextEstimatedChargeTime = "2026-11-01 00:00:00"),
                CurrentBattery(2, nextEstimatedChargeTime = "2026-11-04 20:00:00"),
                CurrentBattery(3, nextEstimatedChargeTime = "2026-11-06 00:00:00"),
                CurrentBattery(4),
            ),
            now = now,
            dueSoonDays = 5,
        )

        assertEquals(BatteriesSummary(count = 4, overdue = 1, dueToday = 1, dueSoon = 2), summary)
    }
}
