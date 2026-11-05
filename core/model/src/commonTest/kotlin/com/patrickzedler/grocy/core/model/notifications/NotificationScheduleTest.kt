package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationScheduleTest {

    private val today = 20_000L
    private val stockAtNine = settingsWith(NotificationKind.STOCK, minute = 9 * 60)
    private val config = ServerConfig()

    private fun settingsWith(
        kind: NotificationKind,
        minute: Int,
        base: NotificationSettings = NotificationSettings(),
    ) = base.with(kind, base[kind].copy(enabled = true, minuteOfDay = minute))

    private fun at(hour: Int, minute: Int = 0, day: Long = today) =
        day * DAY + (hour * 60 + minute) * MINUTE

    @Test
    fun nothingIsDueBeforeItsTime() {
        val due = dueNotificationKinds(stockAtNine, NotificationDeliveries(), config, at(8, 59))

        assertTrue(due.isEmpty())
    }

    @Test
    fun aKindIsDueFromItsMinuteOn() {
        val due = dueNotificationKinds(stockAtNine, NotificationDeliveries(), config, at(9))

        assertEquals(setOf(NotificationKind.STOCK), due)
    }

    @Test
    fun aKindComesOnlyOnceADay() {
        val deliveries = NotificationDeliveries(
            mapOf(NotificationKind.STOCK to NotificationDelivery(day = today)),
        )

        assertTrue(dueNotificationKinds(stockAtNine, deliveries, config, at(9, 1)).isEmpty())
        assertEquals(
            setOf(NotificationKind.STOCK),
            dueNotificationKinds(stockAtNine, deliveries, config, at(9, day = today + 1)),
        )
    }

    @Test
    fun aDeviceOffAtTheTimeCatchesUpTheSameDay() {
        val deliveries = NotificationDeliveries(
            mapOf(NotificationKind.STOCK to NotificationDelivery(day = today - 1)),
        )

        val due = dueNotificationKinds(stockAtNine, deliveries, config, at(22))

        assertEquals(setOf(NotificationKind.STOCK), due)
        assertEquals(at(9), nextNotificationCheck(stockAtNine, deliveries, config, at(22)))
    }

    @Test
    fun aFeatureTurnedOffOnTheServerDoesNotNotify() {
        val off = ServerConfig(stock = false)

        val none = NotificationDeliveries()
        assertTrue(dueNotificationKinds(stockAtNine, none, off, at(10)).isEmpty())
        assertNull(nextNotificationCheck(stockAtNine, none, off, at(10)))
    }

    @Test
    fun nextCheckIsTheEarliestTimeOfAllKinds() {
        val settings = settingsWith(NotificationKind.TASKS, 7 * 60, stockAtNine)

        val next = nextNotificationCheck(settings, NotificationDeliveries(), config, at(6))

        assertEquals(at(7), next)
    }

    @Test
    fun nextCheckIsTomorrowOnceDelivered() {
        val deliveries = NotificationDeliveries(
            mapOf(NotificationKind.STOCK to NotificationDelivery(day = today)),
        )

        assertEquals(
            at(9, day = today + 1),
            nextNotificationCheck(stockAtNine, deliveries, config, at(10)),
        )
    }

    @Test
    fun aRetryComesBeforeTomorrowsTime() {
        val deliveries = NotificationDeliveries(
            mapOf(NotificationKind.STOCK to NotificationDelivery(day = today, retriesLeft = 3)),
            retryAt = at(10, 15),
        )

        assertEquals(at(10, 15), nextNotificationCheck(stockAtNine, deliveries, config, at(10)))
    }

    @Test
    fun nothingEnabledNeedsNoAlarm() {
        assertNull(
            nextNotificationCheck(NotificationSettings(), NotificationDeliveries(), config, at(10)),
        )
    }

    @Test
    fun turningOnAfterTheTimeDoesNotFireAtOnce() {
        val deliveries = deliveriesAfterChange(
            old = NotificationSettings(),
            new = stockAtNine,
            deliveries = NotificationDeliveries(),
            now = at(10),
        )

        assertTrue(dueNotificationKinds(stockAtNine, deliveries, config, at(10)).isEmpty())
    }

    @Test
    fun turningOnBeforeTheTimeComesToday() {
        val deliveries = deliveriesAfterChange(
            old = NotificationSettings(),
            new = stockAtNine,
            deliveries = NotificationDeliveries(
                mapOf(NotificationKind.STOCK to NotificationDelivery(day = today)),
            ),
            now = at(8),
        )

        assertEquals(at(9), nextNotificationCheck(stockAtNine, deliveries, config, at(8)))
    }

    @Test
    fun aLaterTimeComesTodayAgain() {
        val later = settingsWith(NotificationKind.STOCK, minute = 18 * 60)
        val deliveries = deliveriesAfterChange(
            old = stockAtNine,
            new = later,
            deliveries = NotificationDeliveries(
                mapOf(NotificationKind.STOCK to NotificationDelivery(day = today)),
            ),
            now = at(10),
        )

        assertEquals(at(18), nextNotificationCheck(later, deliveries, config, at(10)))
    }

    @Test
    fun otherChangesKeepTheDelivery() {
        val delivered = NotificationDeliveries(
            mapOf(NotificationKind.STOCK to NotificationDelivery(day = today)),
        )
        val changed = stockAtNine.with(
            NotificationKind.STOCK,
            stockAtNine[NotificationKind.STOCK].copy(onlyMine = true),
        )

        assertEquals(delivered, deliveriesAfterChange(stockAtNine, changed, delivered, at(10)))
    }

    @Test
    fun turningOffEndsItsRetries() {
        val deliveries = deliveriesAfterChange(
            old = stockAtNine,
            new = NotificationSettings(),
            deliveries = NotificationDeliveries(
                mapOf(NotificationKind.STOCK to NotificationDelivery(day = today, retriesLeft = 2)),
                retryAt = at(10),
            ),
            now = at(9, 30),
        )

        assertEquals(NotificationDeliveries(), deliveries)
    }

    @Test
    fun deliveriesStoredFormStaysReadable() {
        val deliveries = NotificationDeliveries.of("1200|STOCK=20000:1:0|TASKS=19999:0:3")

        assertEquals(1200L, deliveries.retryAt)
        assertEquals(
            NotificationDelivery(day = 20_000),
            deliveries.byKind[NotificationKind.STOCK],
        )
        assertEquals(
            NotificationDelivery(day = 19_999, shown = false, retriesLeft = 3),
            deliveries.byKind[NotificationKind.TASKS],
        )
        assertEquals(deliveries, NotificationDeliveries.of(deliveries.stored()))
    }

    @Test
    fun unreadableDeliveriesCountAsNeverDelivered() {
        assertEquals(NotificationDeliveries(), NotificationDeliveries.of(null))
        assertEquals(NotificationDeliveries(), NotificationDeliveries.of("x|NEW=1:1:0|STOCK=abc"))
    }

    private companion object {
        const val MINUTE = 60_000L
        const val DAY = 86_400_000L
    }
}
