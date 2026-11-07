package com.patrickzedler.grocy.feature.calendar.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.CalendarOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.calendar.CalendarColorSetting
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.calendar.calendarMonthWeeks
import com.patrickzedler.grocy.core.model.calendar.calendarWeekStart
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.firstDayOfMonth
import com.patrickzedler.grocy.core.model.text.lastDayOfMonth
import com.patrickzedler.grocy.core.model.text.localeFirstDayOfWeek
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.model.text.weekOfYear
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A day the calendar shows; [isOutside] of the month it shows it for. */
internal data class CalendarDay(
    val epochDay: Long,
    val isToday: Boolean,
    val isOutside: Boolean = false,
    val events: List<CalendarEvent>,
)

/** A row of a month, [number] its week of the year. */
internal data class CalendarWeek(val number: Int, val days: List<CalendarDay>)

/**
 * What the calendar shows: [view] around [anchor], the day it stands on. Each page of a view shows
 * the period of a day of its own ([monthWeeks], [gridDays]), the list every day with events.
 */
internal data class CalendarUiState(
    val view: CalendarView,
    val anchor: Long,
    val today: Long,
    val firstDayOfWeek: Int,
    val config: ServerConfig,
    val settings: UserSettings,
    val eventsByDay: Map<Long, List<CalendarEvent>>,
) {
    fun dayOf(epochDay: Long, isOutside: Boolean = false) = CalendarDay(
        epochDay = epochDay,
        isToday = epochDay == today,
        isOutside = isOutside,
        events = eventsByDay[epochDay].orEmpty(),
    )

    fun weekStartOf(epochDay: Long): Long = calendarWeekStart(epochDay, firstDayOfWeek)

    fun weekNumberOf(epochDay: Long): Int = weekOfYear(weekStartOf(epochDay), firstDayOfWeek)

    /** The rows of the month [epochDay] lies in, with the days of other months that fill them. */
    fun monthWeeks(epochDay: Long = anchor): List<CalendarWeek> {
        val first = firstDayOfMonth(epochDay)
        val last = lastDayOfMonth(epochDay)
        return calendarMonthWeeks(epochDay, firstDayOfWeek).map { start ->
            CalendarWeek(
                number = weekOfYear(start, firstDayOfWeek),
                days = (0 until DAYS_PER_WEEK).map { offset ->
                    val shown = start + offset
                    dayOf(shown, isOutside = shown < first || shown > last)
                },
            )
        }
    }

    /** The columns of the time grid of [view] around [epochDay]. */
    fun gridDays(view: CalendarView, epochDay: Long = anchor): List<CalendarDay> = when (view) {
        CalendarView.WEEK -> (0 until DAYS_PER_WEEK).map { dayOf(weekStartOf(epochDay) + it) }
        else -> listOf(dayOf(epochDay))
    }

    /** The colors of the features the server has, in the order of Grocy's dialog. */
    val colorSettings: List<CalendarColorSetting>
        get() = CalendarColorSetting.entries.filter { it.isAvailable(config) }
}

/** Whether [view] shows its days as columns over the hours. */
internal val CalendarView.isTimeGrid: Boolean
    get() = this == CalendarView.DAY || this == CalendarView.WEEK

/** Whether [view] pages through its periods; the list goes on by scrolling. */
internal val CalendarView.isPaged: Boolean
    get() = this != CalendarView.LIST

/**
 * Where events of the [minutes] of a day stand in a time grid, each [chipHeight] high with the
 * time's place at [minuteHeight] per minute: they have no duration, so ones that would overlap
 * follow each other, and those at the day's end move up to stay inside its [totalHeight].
 */
internal fun chipTops(
    minutes: List<Int>,
    minuteHeight: Float,
    chipHeight: Float,
    totalHeight: Float,
): List<Float> {
    val tops = FloatArray(minutes.size)
    minutes.forEachIndexed { index, minute ->
        val wanted = minute * minuteHeight
        tops[index] = if (index == 0) wanted else maxOf(wanted, tops[index - 1] + chipHeight)
    }
    for (index in minutes.indices.reversed()) {
        val limit = totalHeight - (minutes.size - index) * chipHeight
        tops[index] = minOf(tops[index], limit).coerceAtLeast(0f)
    }
    return tops.toList()
}

/** The minute of the day of a time "HH:MM" of an event, null for none or an invalid one. */
internal fun minuteOfTime(time: String?): Int? {
    val parts = time?.split(':') ?: return null
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return (hour * MINUTES_PER_HOUR + minute)
        .takeIf { it in 0 until MINUTES_PER_HOUR * HOURS_PER_DAY }
}

/** How many of [count] events a place for [limit] shows, keeping one row for "more". */
internal fun shownChips(count: Int, limit: Int): Int = if (count <= limit) count else limit - 1

/** What became of an action of [CalendarViewModel], shown once. */
internal sealed interface CalendarEvents {
    data class SharingFailed(val error: ChangeError) : CalendarEvents

    data class ColorFailed(val error: ChangeError) : CalendarEvents
}

/**
 * Grocy's calendar: its events at once from the database, downloaded in the background, in the
 * view the user chose last (kept by [options], the list without one). It opens on [initialDay] in
 * [initialView] if given, e.g. from a widget, else on today. The color settings save like Grocy's:
 * each change on its own, online only.
 */
internal class CalendarViewModel(
    private val calendar: CalendarRepository,
    private val masterData: MasterDataRepository,
    private val options: CalendarOptionsRepository,
    private val today: () -> Long = ::todayEpochDay,
    private val localeFirstDay: () -> Int = ::localeFirstDayOfWeek,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    initialView: CalendarView? = null,
    initialDay: Long? = null,
) : ViewModel() {

    private val anchor = MutableStateFlow(initialDay ?: today())

    // A view of the route counts for this screen only, a chosen one is kept for the next time
    private val shownView = MutableStateFlow(initialView)

    private val refreshController = RefreshController(viewModelScope) { calendar.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val eventChannel = Channel<CalendarEvents>(Channel.BUFFERED)
    val events: Flow<CalendarEvents> = eventChannel.receiveAsFlow()

    private val mutableSharingLink = MutableStateFlow<String?>(null)

    /** The public link while its dialog is open, kept nowhere else because it holds a key. */
    val sharingLink: StateFlow<String?> = mutableSharingLink.asStateFlow()

    val state: StateFlow<CalendarUiState?> = combine(
        calendar.events,
        combine(masterData.serverConfig, masterData.userSettings, ::Pair),
        combine(shownView, options.view) { shown, stored ->
            shown ?: stored ?: CalendarView.Default
        },
        anchor,
    ) { events, (config, settings), view, day ->
        CalendarUiState(
            view = view,
            anchor = day,
            today = today(),
            firstDayOfWeek = config.calendarFirstDayOfWeek ?: localeFirstDay(),
            config = config,
            settings = settings,
            eventsByDay = events.groupBy { it.epochDay },
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    /** Shows [view] and keeps it for the next time the calendar opens. */
    fun chooseView(view: CalendarView) {
        shownView.value = view
        viewModelScope.launch { options.setView(view) }
    }

    /** The calendar stands on [epochDay], e.g. after paging to another period. */
    fun showDay(epochDay: Long) {
        anchor.value = epochDay
    }

    /** Grocy's "Today". */
    fun showToday() {
        anchor.value = today()
    }

    /** Shows the day [epochDay] on its own, what a tap on a day of another view does. */
    fun openDay(epochDay: Long) {
        anchor.value = epochDay
        chooseView(CalendarView.DAY)
    }

    /** Grocy's "Share/Integrate calendar (iCal)": asks the server for the link. */
    fun openSharing() {
        viewModelScope.launch {
            var link: String? = null
            val error = changeErrorOf { link = calendar.sharingLink() }
            if (error != null) {
                eventChannel.send(CalendarEvents.SharingFailed(error))
            } else {
                mutableSharingLink.value = link
            }
        }
    }

    fun closeSharing() {
        mutableSharingLink.value = null
    }

    /** Saves a color of Grocy's "Configure colors", the events take it once the server did. */
    fun saveColor(setting: CalendarColorSetting, color: String) {
        viewModelScope.launch {
            val error = changeErrorOf {
                masterData.saveUserSetting(setting.key, UserSettingValue.Text(color))
            }
            if (error != null) eventChannel.send(CalendarEvents.ColorFailed(error))
        }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
internal const val MINUTES_PER_HOUR = 60
internal const val HOURS_PER_DAY = 24
