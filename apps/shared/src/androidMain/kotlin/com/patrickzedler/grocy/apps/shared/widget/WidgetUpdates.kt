package com.patrickzedler.grocy.apps.shared.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.patrickzedler.grocy.apps.shared.AndroidAppLanguageStore
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.apps.shared.WidgetDependencies
import com.patrickzedler.grocy.apps.shared.utcMillisOfLocal
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.task_change_failed
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

/** State of the widgets the whole process shares. */
internal object WidgetState {
    /** The day the widgets show as today, moved on by [WidgetDayReceiver] at midnight. */
    val today = MutableStateFlow(todayEpochDay())

    /** Tasks whose "Mark task as completed" of a widget is under way. */
    val completing = MutableStateFlow(emptySet<Int>())

    /** Whether any widget is on a home screen; nothing runs for them without one. */
    val hasWidgets = MutableStateFlow(false)
}

/** What the widgets show, anew whenever any of it changes. */
internal fun widgetDataOf(dependencies: WidgetDependencies): Flow<WidgetData> = combine(
    combine(
        dependencies.connection.map { it != null },
        dependencies.masterDataRepository.serverConfig,
        dependencies.masterDataRepository.userSettings,
        ::Triple,
    ),
    combine(
        dependencies.tasksRepository.tasks,
        dependencies.calendarRepository.events,
        ::Pair,
    ),
    combine(dependencies.appearance, WidgetState.today, WidgetState.completing, ::Triple),
) { (isLoggedIn, config, settings), (tasks, events), (appearance, today, completing) ->
    WidgetData(isLoggedIn, config, settings, tasks, events, appearance, today, completing)
}
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)

/**
 * Keeps the widgets up to date and costs nothing without them: they redraw when what they show
 * changes while the app runs, at midnight (an alarm that does not wake the device, it shows the
 * new day once the screen is on) and after what the server changed, which [WidgetRefreshWorker]
 * checks every half hour while there is a network.
 */
internal object WidgetUpdates {

    private val receivers = listOf(
        TaskListWidgetReceiver::class.java,
        MonthWidgetReceiver::class.java,
        ScheduleWidgetReceiver::class.java,
    )

    private val widgets: List<GlanceAppWidget>
        get() = listOf(TaskListWidget(), MonthWidget(), ScheduleWidget())

    /** Starts redrawing the widgets on changes while the app runs in [scope]. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start(context: Context, dependencies: WidgetDependencies, scope: CoroutineScope) {
        scope.launch {
            sync(context)
            redrawAfterLanguageChange(context)
            WidgetState.hasWidgets
                .flatMapLatest { has ->
                    // What they show right now is drawn already
                    if (has) widgetDataOf(dependencies).drop(1) else emptyFlow()
                }
                // Changes come in bursts, e.g. a download that stores several tables
                .debounce(REDRAW_DELAY_MILLIS)
                .collect { updateAll(context) }
        }
    }

    /** Sets up or cancels what keeps them current, by whether any widget is on a home screen. */
    fun sync(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val has = receivers.any {
            manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
        }
        WidgetState.hasWidgets.value = has
        val workManager = WorkManager.getInstance(context)
        if (has) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                REFRESH_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            ).build()
            workManager.enqueueUniquePeriodicWork(
                REFRESH_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            setMidnightAlarm(context)
        } else {
            workManager.cancelUniqueWork(REFRESH_WORK)
            context.getSystemService(AlarmManager::class.java).cancel(midnightIntent(context))
        }
    }

    suspend fun updateAll(context: Context) {
        widgets.forEach { it.updateAll(context) }
    }

    /** A new language restarts the app, the widgets keep their texts until they are drawn. */
    private suspend fun redrawAfterLanguageChange(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val language = Locale.getDefault().toLanguageTag()
        if (preferences.getString(LANGUAGE, null) == language) return
        preferences.edit().putString(LANGUAGE, language).apply()
        if (WidgetState.hasWidgets.value) updateAll(context)
    }

    // Does not wake the device: the new day only matters once someone looks at the screen
    private fun setMidnightAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).set(
            AlarmManager.RTC,
            nextMidnightMillis(todayEpochDay(), TimeZone.getDefault()),
            midnightIntent(context),
        )
    }

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetDayReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private const val REFRESH_WORK = "widget_refresh"
    private const val REFRESH_INTERVAL_MINUTES = 30L
    private const val REDRAW_DELAY_MILLIS = 500L
    private const val PREFERENCES = "widgets"
    private const val LANGUAGE = "language"
}

/** The instant the day after [today] starts in [zone]. */
internal fun nextMidnightMillis(today: Long, zone: TimeZone): Long =
    utcMillisOfLocal((today + 1) * MILLIS_PER_DAY, zone)

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Downloads what the widgets show when the server changed something since the last time, so a
 * check without news costs one small request. Offline or failing it waits for the next run.
 */
class WidgetRefreshWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val graph = (applicationContext as? AppGraphOwner)?.graph ?: return Result.success()
        try {
            val downloaded = graph.widgetDependencies.calendarRepository
                .refreshIfChanged(todayEpochDay())
            // The app's own redraw may have started after the download stored it
            if (downloaded) WidgetUpdates.updateAll(applicationContext)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next run tries again, retrying sooner would cost more than it brings
        }
        return Result.success()
    }
}

/**
 * Moves the widgets to the new day at midnight and sets up what keeps them current again after a
 * restart, an update of the app, a changed time, zone or language of the device.
 */
class WidgetDayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AndroidAppLanguageStore.wrap(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                WidgetState.today.value = todayEpochDay()
                WidgetUpdates.sync(context)
                if (WidgetState.hasWidgets.value) WidgetUpdates.updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** The base of the widgets' receivers: the first and the last widget start and stop the updates. */
abstract class GrocyWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdates.sync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdates.sync(context)
    }
}

/** Grocy's "Mark task as completed" of the task list widget, online only like in the app. */
class CompleteTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val graph = (context.applicationContext as? AppGraphOwner)?.graph ?: return
        // A second tap while it is under way does nothing
        if (taskId in WidgetState.completing.value) return
        WidgetState.completing.update { it + taskId }
        try {
            graph.widgetDependencies.tasksRepository.complete(
                taskId,
                grocyDateTimeOf(localNowMillis()),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            AndroidAppLanguageStore.wrap(context)
            val message = getString(Res.string.task_change_failed)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        } finally {
            WidgetState.completing.update { it - taskId }
        }
    }
}

/** Grocy's "prev" and "next" of the month widget, kept for the rest of the day. */
class MonthStepAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val steps = parameters[StepsKey] ?: return
        val today = todayEpochDay()
        updateAppWidgetState(context, glanceId) { state ->
            val offset = shownMonthOffset(state[MonthOffsetKey] ?: 0, state[SteppedOnKey], today)
            state[MonthOffsetKey] = offset + steps
            state[SteppedOnKey] = today
        }
        MonthWidget().update(context, glanceId)
    }
}

internal val TaskIdKey = ActionParameters.Key<Int>("task_id")
internal val StepsKey = ActionParameters.Key<Int>("steps")
internal val MonthOffsetKey = intPreferencesKey("month_offset")
internal val SteppedOnKey = longPreferencesKey("stepped_on")
