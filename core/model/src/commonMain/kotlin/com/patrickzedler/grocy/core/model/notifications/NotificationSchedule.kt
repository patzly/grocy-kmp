package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.server.ServerConfig

/**
 * What was delivered for one kind: the last [day] (days since 1970-01-01), whether something was
 * [shown] and how many more tries to get the server's state after it could not be reached.
 */
data class NotificationDelivery(
    val day: Long,
    val shown: Boolean = true,
    val retriesLeft: Int = 0,
)

/**
 * What was delivered per kind and when to try again to reach the server ([retryAt], local
 * milliseconds like `localNowMillis`). Stored, so it survives the process and restarts.
 */
data class NotificationDeliveries(
    val byKind: Map<NotificationKind, NotificationDelivery> = emptyMap(),
    val retryAt: Long? = null,
) {
    /** What [of] reads back: "retry|KIND=day:shown:retries|...", the retry empty without one. */
    fun stored(): String =
        (listOf(retryAt?.toString().orEmpty()) + byKind.map { (kind, delivery) ->
            "${kind.name}=${delivery.day}:${if (delivery.shown) 1 else 0}:${delivery.retriesLeft}"
        }).joinToString("|")

    companion object {
        /** Reads [stored]; parts that cannot be read count as never delivered. */
        fun of(stored: String?): NotificationDeliveries {
            val parts = stored.orEmpty().split("|")
            val kinds = parts.drop(1).mapNotNull { part ->
                val kind = NotificationKind.entries
                    .firstOrNull { it.name == part.substringBefore('=') }
                val values = part.substringAfter('=').split(":")
                val day = values.firstOrNull()?.toLongOrNull()
                if (kind == null || day == null) {
                    null
                } else {
                    kind to NotificationDelivery(
                        day = day,
                        shown = values.getOrNull(1) != "0",
                        retriesLeft = values.getOrNull(2)?.toIntOrNull() ?: 0,
                    )
                }
            }
            return NotificationDeliveries(kinds.toMap(), parts.first().toLongOrNull())
        }
    }
}

/** The day of [localMillis] (see `localNowMillis`). */
fun notificationDayOf(localMillis: Long): Long = localMillis.floorDiv(MILLIS_PER_DAY)

private fun NotificationKindSettings.timeOn(day: Long): Long =
    day * MILLIS_PER_DAY + minuteOfDay * MILLIS_PER_MINUTE

private fun NotificationSettings.scheduledKinds(config: ServerConfig) =
    enabledKinds.filter { it.isAvailable(config) }

/**
 * The kinds to notify about at [now]: their time passed today and nothing was delivered yet,
 * whether the alarm just fired or the device was off when it should have.
 */
fun dueNotificationKinds(
    settings: NotificationSettings,
    deliveries: NotificationDeliveries,
    config: ServerConfig,
    now: Long,
): Set<NotificationKind> {
    val today = notificationDayOf(now)
    return settings.scheduledKinds(config).filterTo(mutableSetOf()) { kind ->
        val delivered = deliveries.byKind[kind]?.day
        (delivered == null || delivered < today) && settings[kind].timeOn(today) <= now
    }
}

/**
 * When to look again at the earliest, local milliseconds: the next time of a kind or the next try
 * to reach the server; at or before [now] when something is due already, null with nothing to do.
 */
fun nextNotificationCheck(
    settings: NotificationSettings,
    deliveries: NotificationDeliveries,
    config: ServerConfig,
    now: Long,
): Long? {
    val today = notificationDayOf(now)
    val times = settings.scheduledKinds(config).map { kind ->
        val delivered = deliveries.byKind[kind]?.day
        val day = if (delivered != null && delivered >= today) today + 1 else today
        settings[kind].timeOn(day)
    }
    return (times + listOfNotNull(deliveries.retryAt)).minOrNull()
}

/**
 * [deliveries] after the settings changed from [old] to [new]: a kind turned on or moved to
 * another time starts anew, so it never fires at once for a time that passed already today, but
 * does today for one still to come.
 */
fun deliveriesAfterChange(
    old: NotificationSettings,
    new: NotificationSettings,
    deliveries: NotificationDeliveries,
    now: Long,
): NotificationDeliveries {
    val today = notificationDayOf(now)
    val byKind = deliveries.byKind.toMutableMap()
    NotificationKind.entries.forEach { kind ->
        val changed = new[kind].enabled && (
            !old[kind].enabled || old[kind].minuteOfDay != new[kind].minuteOfDay
        )
        if (changed) {
            if (new[kind].timeOn(today) <= now) {
                byKind[kind] = NotificationDelivery(day = today)
            } else {
                byKind.remove(kind)
            }
        } else if (!new[kind].enabled) {
            byKind.remove(kind)
        }
    }
    return NotificationDeliveries(
        byKind = byKind,
        retryAt = deliveries.retryAt.takeIf { byKind.values.any { it.retriesLeft > 0 } },
    )
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_DAY = 86_400_000L
