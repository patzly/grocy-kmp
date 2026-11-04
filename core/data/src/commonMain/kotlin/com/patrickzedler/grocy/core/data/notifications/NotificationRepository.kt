package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationDelivery
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.deliveriesAfterChange
import com.patrickzedler.grocy.core.model.notifications.dueNotificationKinds
import com.patrickzedler.grocy.core.model.notifications.nextNotificationCheck
import com.patrickzedler.grocy.core.model.notifications.notificationDayOf
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.localNowMillis
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What [NotificationRepository.showNow] did. */
enum class ShowNowResult { SHOWN, NOTHING_TO_TELL, FAILED }

/**
 * The daily notifications: when to show them, what they say and that each comes once a day.
 *
 * Everything worth keeping is stored, so it does not matter whether the process lived on since
 * the alarm was set. An alarm that never fired (the device was off, the app force stopped) is
 * caught up on the next [check], e.g. at boot or when the app opens. A notification never waits
 * for the network: when the server cannot be reached it shows the last known state marked as
 * such and tries again [MAX_RETRIES] times, updating it without alerting anew.
 *
 * @param connection the logged in server, null after a logout.
 * @param now local time like `localNowMillis`.
 */
class NotificationRepository(
    private val settingsStore: SettingsStore,
    private val connection: Flow<ServerConnection?>,
    private val serverConfig: Flow<ServerConfig>,
    private val dataSource: NotificationDataSource,
    private val notifier: Notifier,
    private val alarm: NotificationAlarm,
    private val scope: CoroutineScope,
    private val now: () -> Long = ::localNowMillis,
) {

    /** Null only for the moment at app start until the stored settings are read. */
    val settings: StateFlow<NotificationSettings?> =
        settingsStore.notificationSettings.stateIn(scope, SharingStarted.Eagerly, null)

    // The alarm, the app starting and a change of the settings may all check at the same time
    private val checks = Mutex()

    /** Keeps the alarm right from now on, also after a login and when the features change. */
    fun start() {
        scope.launch {
            combine(
                settingsStore.notificationSettings,
                connection.map { it != null },
                serverConfig,
                ::Triple,
            ).collect { check() }
        }
    }

    /** Changes the settings; a kind turned off takes its notification away. */
    suspend fun update(transform: (NotificationSettings) -> NotificationSettings) {
        checks.withLock {
            val old = settingsStore.readNotificationSettings()
            val new = transform(old)
            settingsStore.setNotificationDeliveries(
                deliveriesAfterChange(old, new, settingsStore.readNotificationDeliveries(), now()),
            )
            settingsStore.setNotificationSettings(new)
            (old.enabledKinds - new.enabledKinds).forEach { notifier.cancel(it) }
        }
    }

    /**
     * Notifies about what is due now and sets the alarm for what comes next. Called by the alarm,
     * at boot and whenever anything it depends on changes.
     */
    suspend fun check() {
        checks.withLock {
            try {
                checkDue()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The alarm fired only once: without a new one nothing would come anymore
                alarm.set(now() + RETRY_DELAY_MILLIS)
            }
        }
    }

    private suspend fun checkDue() {
        if (connection.first() == null) {
            signedOut()
            return
        }
        val config = serverConfig.first()
        val settings = settingsStore.readNotificationSettings()
        var deliveries = settingsStore.readNotificationDeliveries()
        val time = now()
        val due = dueNotificationKinds(settings, deliveries, config, time)
        val retrying = deliveries.retryAt?.takeIf { it <= time }?.let {
            deliveries.byKind.filterValues { delivery -> delivery.retriesLeft > 0 }.keys
        }.orEmpty()
        if (due.isNotEmpty() || retrying.isNotEmpty()) {
            deliveries = notify(due, retrying - due, settings, deliveries, time)
        } else if (deliveries.retryAt?.let { it <= time } == true) {
            // Nothing left to retry: a time in the past would fire the alarm again and again
            deliveries = deliveries.copy(retryAt = null)
            settingsStore.setNotificationDeliveries(deliveries)
        }
        when (val next = nextNotificationCheck(settings, deliveries, config, time)) {
            null -> alarm.cancel()
            else -> alarm.set(next)
        }
    }

    // Counts as delivered today, so the next login does not notify about a time long past
    private suspend fun signedOut() {
        alarm.cancel()
        val today = notificationDayOf(now())
        val delivered = NotificationDelivery(day = today, shown = false)
        val signedOut = NotificationDeliveries(NotificationKind.entries.associateWith { delivered })
        if (settingsStore.readNotificationDeliveries() != signedOut) {
            NotificationKind.entries.forEach { notifier.cancel(it) }
            settingsStore.setNotificationDeliveries(signedOut)
        }
    }

    private suspend fun notify(
        due: Set<NotificationKind>,
        retrying: Set<NotificationKind>,
        settings: NotificationSettings,
        deliveries: NotificationDeliveries,
        time: Long,
    ): NotificationDeliveries {
        val fresh = dataSource.refresh(due + retrying)
        // The features as the server says now, one turned off meanwhile does not notify anymore
        val config = dataSource.serverConfig()
        val today = notificationDayOf(time)
        val byKind = deliveries.byKind.toMutableMap()

        due.forEach { kind ->
            if (!kind.isAvailable(config)) {
                notifier.cancel(kind)
                byKind[kind] = NotificationDelivery(day = today, shown = false)
                return@forEach
            }
            val isFresh = kind in fresh
            val shown = showOrCancel(kind, settings, time, isStale = !isFresh, silently = false)
            byKind[kind] = NotificationDelivery(
                day = today,
                shown = shown,
                retriesLeft = if (isFresh) 0 else MAX_RETRIES,
            )
            // Stored at once: a process killed now must not alert again
            settingsStore.setNotificationDeliveries(deliveries.copy(byKind = byKind.toMap()))
        }
        retrying.forEach { kind ->
            val delivery = byKind.getValue(kind)
            byKind[kind] = when {
                kind !in fresh -> delivery.copy(retriesLeft = delivery.retriesLeft - 1)
                // The user dismissed it after reading it, it stays away
                delivery.shown && !notifier.isShowing(kind) -> delivery.copy(retriesLeft = 0)
                else -> delivery.copy(
                    // Alerts only when nothing was there to alert about before
                    shown = showOrCancel(
                        kind = kind,
                        settings = settings,
                        time = time,
                        isStale = false,
                        silently = delivery.shown,
                    ),
                    retriesLeft = 0,
                )
            }
        }

        val updated = NotificationDeliveries(
            byKind = byKind,
            retryAt = (time + RETRY_DELAY_MILLIS).takeIf {
                byKind.values.any { it.retriesLeft > 0 }
            },
        )
        settingsStore.setNotificationDeliveries(updated)
        return updated
    }

    /** Returns whether something is shown: with nothing to tell the old one goes away too. */
    private suspend fun showOrCancel(
        kind: NotificationKind,
        settings: NotificationSettings,
        time: Long,
        isStale: Boolean,
        silently: Boolean,
    ): Boolean {
        val summary = try {
            dataSource.summary(kind, settings[kind], time)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return false
        }
        return if (summary.sections.isEmpty()) {
            notifier.cancel(kind)
            false
        } else {
            notifier.show(summary.copy(isStale = isStale), silently)
        }
    }

    /**
     * Shows what the notification of [kind] would say right now, whatever its time is. The one
     * already there goes first: platforms replace it without a sound or a popup, which for a tap
     * on "Show now" would look as if nothing happened.
     */
    suspend fun showNow(kind: NotificationKind): ShowNowResult {
        val fresh = dataSource.refresh(setOf(kind))
        val settings = settingsStore.readNotificationSettings()
        return try {
            val summary = dataSource.summary(kind, settings[kind], now())
            notifier.cancel(kind)
            when {
                summary.sections.isEmpty() -> ShowNowResult.NOTHING_TO_TELL
                notifier.show(summary.copy(isStale = kind !in fresh), silently = false) ->
                    ShowNowResult.SHOWN
                else -> ShowNowResult.FAILED
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ShowNowResult.FAILED
        }
    }

    private companion object {
        const val MAX_RETRIES = 8
        const val RETRY_DELAY_MILLIS = 15 * 60_000L
    }
}
