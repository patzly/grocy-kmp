package com.patrickzedler.grocy.core.model.batteries

import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.choreDueStatusOf

/**
 * An active battery as Grocy computes it for its overview (`/batteries`). Times are Grocy's
 * "YYYY-MM-DD HH:MM:SS".
 */
data class CurrentBattery(
    val batteryId: Int,
    val lastTrackedTime: String? = null,
    /** Null without a charge cycle interval or before the first charge cycle. */
    val nextEstimatedChargeTime: String? = null,
)

/** One row of Grocy's batteries journal (`battery_charge_cycles`). */
data class BatteryChargeCycle(
    val id: Int,
    val batteryId: Int,
    val trackedTime: String,
    val undone: Boolean = false,
    val undoneTimestamp: String? = null,
    val userfields: Map<String, String?> = emptyMap(),
)

/** What Grocy's "Battery overview" card shows, as the server computed it (`/batteries/{id}`). */
data class BatteryDetails(
    val batteryId: Int,
    val lastCharged: String? = null,
    val chargeCyclesCount: Int = 0,
)

/** How close a battery's next charge cycle is, Grocy's row highlighting and status filter. */
enum class BatteryDueStatus { OVERDUE, DUE_TODAY, DUE_SOON }

/**
 * The status of a battery due at [next] like Grocy's overview at [now] (both Grocy's "YYYY-MM-DD
 * HH:MM:SS"); null when it is not due yet or has no next charge cycle. Grocy compares chores and
 * batteries the same way.
 */
fun batteryDueStatusOf(next: String?, now: String, dueSoonDays: Int): BatteryDueStatus? =
    when (choreDueStatusOf(next, now, dueSoonDays)) {
        ChoreDueStatus.OVERDUE -> BatteryDueStatus.OVERDUE
        ChoreDueStatus.DUE_TODAY -> BatteryDueStatus.DUE_TODAY
        ChoreDueStatus.DUE_SOON -> BatteryDueStatus.DUE_SOON
        null -> null
    }

/** Grocy's messages above its overview; [dueSoon] counts the ones due today as well. */
data class BatteriesSummary(
    val count: Int = 0,
    val overdue: Int = 0,
    val dueToday: Int = 0,
    val dueSoon: Int = 0,
)

/** Counts [batteries] like Grocy's messages. */
fun batteriesSummaryOf(
    batteries: List<CurrentBattery>,
    now: String,
    dueSoonDays: Int,
): BatteriesSummary {
    val statuses = batteries.map { batteryDueStatusOf(it.nextEstimatedChargeTime, now, dueSoonDays) }
    val dueToday = statuses.count { it == BatteryDueStatus.DUE_TODAY }
    return BatteriesSummary(
        count = batteries.size,
        overdue = statuses.count { it == BatteryDueStatus.OVERDUE },
        dueToday = dueToday,
        dueSoon = dueToday + statuses.count { it == BatteryDueStatus.DUE_SOON },
    )
}
