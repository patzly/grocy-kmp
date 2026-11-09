package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationDelivery
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationKindSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationSection
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChangeNames
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class NotificationRepositoryTest {

    private val today = 20_000L
    private val connected = ServerConnection.ApiKey("https://grocy.test", "key")

    private class FakeNotifier : Notifier {
        val shown = mutableListOf<Pair<NotificationSummary, Boolean>>()
        val showing = mutableSetOf<NotificationKind>()
        val cancelled = mutableListOf<NotificationKind>()

        var isBlocked = false

        override suspend fun show(summary: NotificationSummary, silently: Boolean): Boolean {
            if (isBlocked) return false
            shown += summary to silently
            showing += summary.kind
            return true
        }

        override suspend fun cancel(kind: NotificationKind) {
            cancelled += kind
            showing -= kind
        }

        override suspend fun isShowing(kind: NotificationKind) = kind in showing

        override suspend fun showShoppingListChanges(names: ShoppingListChangeNames) = true

        override suspend fun cancelShoppingListChanges() = Unit

        override suspend fun isShowingShoppingListChanges() = false
    }

    private class FakeAlarm : NotificationAlarm {
        var time: Long? = null

        override fun set(localMillis: Long) {
            time = localMillis
        }

        override fun cancel() {
            time = null
        }
    }

    private class FakeSource : NotificationDataSource {
        var online = true
        var config = ServerConfig()
        var names = listOf("Milk")
        var failing = false
        val refreshed = mutableListOf<Set<NotificationKind>>()

        override suspend fun refresh(kinds: Set<NotificationKind>): Set<NotificationKind> {
            refreshed += kinds
            return if (online) kinds else emptySet()
        }

        override suspend fun serverConfig() = config

        override suspend fun summary(
            kind: NotificationKind,
            settings: NotificationKindSettings,
            now: Long,
        ): NotificationSummary {
            if (failing) error("Database is gone")
            return NotificationSummary(
                kind = kind,
                sections = listOf(NotificationSection(NotificationTopic.STOCK_OVERDUE, names))
                    .filter { names.isNotEmpty() },
            )
        }
    }

    private inner class Environment(
        scope: TestScope,
        settings: NotificationSettings = stockAtNine(),
        deliveries: NotificationDeliveries = NotificationDeliveries(),
        connection: ServerConnection? = connected,
    ) {
        var time = at(9)
        val store = FakeSettingsStore(
            notificationSettings = settings,
            notificationDeliveries = deliveries,
        )
        val notifier = FakeNotifier()
        val alarm = FakeAlarm()
        val source = FakeSource()
        val connectionState = MutableStateFlow(connection)
        val config = MutableStateFlow(ServerConfig())
        val repository = NotificationRepository(
            settingsStore = store,
            connection = connectionState,
            serverConfig = config,
            dataSource = source,
            notifier = notifier,
            alarm = alarm,
            scope = scope.backgroundScope + UnconfinedTestDispatcher(scope.testScheduler),
            now = { time },
        )
    }

    private fun stockAtNine() = NotificationSettings().let {
        it.with(
            NotificationKind.STOCK,
            it[NotificationKind.STOCK].copy(enabled = true, minuteOfDay = 9 * 60),
        )
    }

    private fun at(hour: Int, minute: Int = 0, day: Long = today) =
        day * 86_400_000L + (hour * 60 + minute) * 60_000L

    @Test
    fun beforeItsTimeOnlyTheAlarmIsSet() = runTest {
        val environment = Environment(this).apply { time = at(8) }

        environment.repository.check()

        assertTrue(environment.notifier.shown.isEmpty())
        assertTrue(environment.source.refreshed.isEmpty())
        assertEquals(at(9), environment.alarm.time)
    }

    @Test
    fun atItsTimeTheCurrentStateIsDownloadedBeforeNotifying() = runTest {
        val environment = Environment(this)

        environment.repository.check()

        assertEquals(listOf(setOf(NotificationKind.STOCK)), environment.source.refreshed)
        val (summary, silently) = environment.notifier.shown.single()
        assertEquals(NotificationKind.STOCK, summary.kind)
        assertFalse(summary.isStale)
        assertFalse(silently)
        assertEquals(at(9, day = today + 1), environment.alarm.time)
    }

    @Test
    fun aSecondCheckDoesNotNotifyAgain() = runTest {
        val environment = Environment(this)

        environment.repository.check()
        environment.repository.check()

        assertEquals(1, environment.notifier.shown.size)
    }

    @Test
    fun deliveryIsKeptAcrossProcesses() = runTest {
        val first = Environment(this)
        first.repository.check()
        val stored = first.store.readNotificationDeliveries()

        val second = Environment(this, deliveries = stored)
        second.repository.check()

        assertTrue(second.notifier.shown.isEmpty())
    }

    @Test
    fun whatWasMissedComesLaterTheSameDay() = runTest {
        val environment = Environment(this).apply { time = at(21) }

        environment.repository.check()

        assertEquals(1, environment.notifier.shown.size)
    }

    @Test
    fun nothingToTellTakesTheOldNotificationAway() = runTest {
        val environment = Environment(this)
        environment.notifier.showing += NotificationKind.STOCK
        environment.source.names = emptyList()

        environment.repository.check()

        assertTrue(environment.notifier.shown.isEmpty())
        assertEquals(listOf(NotificationKind.STOCK), environment.notifier.cancelled)
        val delivery = environment.store.readNotificationDeliveries().byKind.values.single()
        assertEquals(today, delivery.day)
    }

    @Test
    fun offlineItShowsTheLastKnownStateAndRetries() = runTest {
        val environment = Environment(this)
        environment.source.online = false

        environment.repository.check()

        val (summary, _) = environment.notifier.shown.single()
        assertTrue(summary.isStale)
        assertEquals(at(9, 15), environment.alarm.time)
    }

    @Test
    fun theRetryUpdatesItWithoutAlertingAgain() = runTest {
        val environment = Environment(this)
        environment.source.online = false
        environment.repository.check()

        environment.source.online = true
        environment.time = at(9, 15)
        environment.repository.check()

        assertEquals(2, environment.notifier.shown.size)
        val (summary, silently) = environment.notifier.shown.last()
        assertFalse(summary.isStale)
        assertTrue(silently)
        assertNull(environment.store.readNotificationDeliveries().retryAt)
        assertEquals(at(9, day = today + 1), environment.alarm.time)
    }

    @Test
    fun aDismissedNotificationStaysAwayAfterTheRetry() = runTest {
        val environment = Environment(this)
        environment.source.online = false
        environment.repository.check()
        environment.notifier.showing.clear()

        environment.source.online = true
        environment.time = at(9, 15)
        environment.repository.check()

        assertEquals(1, environment.notifier.shown.size)
    }

    @Test
    fun whatWasUnknownOfflineStillAlertsOnce() = runTest {
        val environment = Environment(this)
        environment.source.online = false
        environment.source.names = emptyList()
        environment.repository.check()

        environment.source.online = true
        environment.source.names = listOf("Milk")
        environment.time = at(9, 15)
        environment.repository.check()

        val (summary, silently) = environment.notifier.shown.single()
        assertFalse(summary.isStale)
        assertFalse(silently)
    }

    @Test
    fun retriesEndAfterSomeTries() = runTest {
        val environment = Environment(this)
        environment.source.online = false
        environment.repository.check()

        var tries = 0
        while (environment.store.readNotificationDeliveries().retryAt != null && tries < 100) {
            environment.time += 15 * 60_000L
            environment.repository.check()
            tries++
        }

        assertEquals(8, tries)
        assertEquals(at(9, day = today + 1), environment.alarm.time)
    }

    @Test
    fun aFeatureTurnedOffOnTheServerNoLongerNotifies() = runTest {
        val environment = Environment(this)
        environment.source.config = ServerConfig(stock = false)

        environment.repository.check()

        assertTrue(environment.notifier.shown.isEmpty())
        assertEquals(listOf(NotificationKind.STOCK), environment.notifier.cancelled)
    }

    @Test
    fun aFeatureTheServerNeverHadIsNotDownloaded() = runTest {
        val environment = Environment(this)
        environment.config.value = ServerConfig(stock = false)

        environment.repository.check()

        assertTrue(environment.source.refreshed.isEmpty())
        assertNull(environment.alarm.time)
    }

    @Test
    fun everyDueKindIsNotifiedInOneCheck() = runTest {
        val both = stockAtNine().let {
            it.with(
                NotificationKind.TASKS,
                it[NotificationKind.TASKS].copy(enabled = true, minuteOfDay = 8 * 60),
            )
        }
        val environment = Environment(this, settings = both)

        environment.repository.check()

        assertEquals(
            setOf(NotificationKind.STOCK, NotificationKind.TASKS),
            environment.notifier.shown.map { it.first.kind }.toSet(),
        )
        assertEquals(
            listOf(setOf(NotificationKind.STOCK, NotificationKind.TASKS)),
            environment.source.refreshed,
        )
    }

    @Test
    fun aLogoutTakesEverythingAway() = runTest {
        val environment = Environment(this, connection = null)
        environment.alarm.time = at(10)

        environment.repository.check()

        assertNull(environment.alarm.time)
        assertEquals(NotificationKind.entries.toList(), environment.notifier.cancelled)
    }

    @Test
    fun theNextLoginDoesNotNotifyAboutATimeLongPast() = runTest {
        val environment = Environment(this, connection = null).apply { time = at(12) }
        environment.repository.check()

        environment.connectionState.value = connected
        environment.repository.check()

        assertTrue(environment.notifier.shown.isEmpty())
        assertEquals(at(9, day = today + 1), environment.alarm.time)
    }

    @Test
    fun anUnexpectedErrorKeepsTheAlarmGoing() = runTest {
        val environment = Environment(this)
        environment.notifier.showing.clear()
        val throwing = object : Notifier by environment.notifier {
            override suspend fun show(summary: NotificationSummary, silently: Boolean): Boolean =
                error("Boom")
        }
        val repository = NotificationRepository(
            settingsStore = environment.store,
            connection = environment.connectionState,
            serverConfig = environment.config,
            dataSource = environment.source,
            notifier = throwing,
            alarm = environment.alarm,
            scope = backgroundScope,
            now = { environment.time },
        )

        repository.check()

        assertEquals(at(9, 15), environment.alarm.time)
    }

    @Test
    fun turningOffTakesItsNotificationAway() = runTest {
        val environment = Environment(this)

        environment.repository.update {
            it.with(NotificationKind.STOCK, it[NotificationKind.STOCK].copy(enabled = false))
        }

        assertEquals(listOf(NotificationKind.STOCK), environment.notifier.cancelled)
        assertTrue(environment.store.readNotificationSettings().enabledKinds.isEmpty())
    }

    @Test
    fun turningOnAfterTheTimeDoesNotNotifyAtOnce() = runTest {
        val environment = Environment(this, settings = NotificationSettings())
        environment.time = at(12)

        environment.repository.update {
            it.with(
                NotificationKind.STOCK,
                it[NotificationKind.STOCK].copy(enabled = true, minuteOfDay = 9 * 60),
            )
        }
        environment.repository.check()

        assertTrue(environment.notifier.shown.isEmpty())
        assertEquals(at(9, day = today + 1), environment.alarm.time)
    }

    @Test
    fun startKeepsTheAlarmRightWhenTheSettingsChange() = runTest {
        val environment = Environment(this, settings = NotificationSettings())
        environment.time = at(8)
        environment.repository.start()
        testScheduler.advanceUntilIdle()
        assertNull(environment.alarm.time)

        environment.repository.update {
            it.with(
                NotificationKind.TASKS,
                it[NotificationKind.TASKS].copy(enabled = true, minuteOfDay = 10 * 60),
            )
        }
        testScheduler.advanceUntilIdle()

        assertEquals(at(10), environment.alarm.time)
    }

    @Test
    fun showNowTellsWhetherThereIsAnythingAndLeavesTheDailyOneAlone() = runTest {
        val environment = Environment(this).apply { time = at(8) }

        assertEquals(ShowNowResult.SHOWN, environment.repository.showNow(NotificationKind.STOCK))
        environment.source.names = emptyList()
        assertEquals(
            ShowNowResult.NOTHING_TO_TELL,
            environment.repository.showNow(NotificationKind.STOCK),
        )

        assertEquals(NotificationDeliveries(), environment.store.readNotificationDeliveries())
    }

    @Test
    fun showNowAlertsAgainInsteadOfSilentlyReplacingTheOneThere() = runTest {
        val environment = Environment(this).apply { time = at(8) }
        environment.repository.showNow(NotificationKind.STOCK)
        environment.notifier.cancelled.clear()

        environment.repository.showNow(NotificationKind.STOCK)

        assertEquals(listOf(NotificationKind.STOCK), environment.notifier.cancelled)
        assertEquals(2, environment.notifier.shown.size)
        assertFalse(environment.notifier.shown.last().second)
    }

    @Test
    fun showNowReportsWhenThePlatformDoesNotShowIt() = runTest {
        val environment = Environment(this).apply { time = at(8) }
        environment.notifier.isBlocked = true

        assertEquals(ShowNowResult.FAILED, environment.repository.showNow(NotificationKind.STOCK))
    }

    @Test
    fun aKindDeliveredYesterdayIsDueAgain() = runTest {
        val yesterday = NotificationDeliveries(
            mapOf(NotificationKind.STOCK to NotificationDelivery(day = today - 1)),
        )
        val environment = Environment(this, deliveries = yesterday)

        environment.repository.check()

        assertEquals(1, environment.notifier.shown.size)
    }
}
