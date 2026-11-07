package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.calendar.CalendarView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The calendar's view the user chose last, read once at app start in [scope], so the calendar
 * opens in it in its first frame.
 */
class CalendarOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored view is read. */
    val view: StateFlow<CalendarView?> =
        settingsStore.calendarView.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setView(view: CalendarView) {
        settingsStore.setCalendarView(view)
    }
}
