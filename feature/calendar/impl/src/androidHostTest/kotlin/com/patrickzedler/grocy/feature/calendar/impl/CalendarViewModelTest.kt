package com.patrickzedler.grocy.feature.calendar.impl

import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.model.calendar.CalendarColorSetting
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.data.settings.CalendarOptionsRepository
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val today = epochDayOf("2026-11-04")!!
    private val settingsStore = FakeSettingsStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        initialView: CalendarView? = null,
        initialDay: Long? = null,
    ): CalendarViewModel {
        val viewModel = CalendarViewModel(
            calendar = environment.calendarRepository,
            masterData = environment.repository,
            options = CalendarOptionsRepository(
                settingsStore,
                backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
            today = { today },
            localeFirstDay = { 1 },
            computeDispatcher = dispatcher,
            initialView = initialView,
            initialDay = initialDay,
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun aWidgetOpensItOnItsDayInItsViewWithoutKeepingIt() = runTest(dispatcher) {
        val day = epochDayOf("2026-12-24")!!

        val state = assertNotNull(viewModel(CalendarView.MONTH, day).state.value)

        assertEquals(day, state.anchor)
        assertEquals(CalendarView.MONTH, state.view)
        assertEquals(CalendarView.LIST, settingsStore.calendarView.value)
    }

    @Test
    fun itOpensOnTodayInTheListAndDownloads() = runTest(dispatcher) {
        val viewModel = viewModel()

        val state = assertNotNull(viewModel.state.value)

        assertEquals(today, state.anchor)
        assertEquals(CalendarView.LIST, state.view)
        assertTrue(environment.server.requestedPaths.contains("/api/chores"))
    }

    @Test
    fun theChosenViewIsKeptForTheNextTime() = runTest(dispatcher) {
        viewModel().chooseView(CalendarView.WEEK)
        advanceUntilIdle()

        assertEquals(CalendarView.WEEK, settingsStore.calendarView.value)
        assertEquals(CalendarView.WEEK, viewModel().state.value!!.view)
    }

    @Test
    fun todayComesBackFromAnywhere() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.showDay(today + 40)
        advanceUntilIdle()

        viewModel.showToday()
        advanceUntilIdle()

        assertEquals(today, viewModel.state.value!!.anchor)
    }

    @Test
    fun theMonthMarksDaysOfOtherMonths() = runTest(dispatcher) {
        val viewModel = viewModel()

        val weeks = viewModel.state.value!!.monthWeeks()

        assertEquals("2026-10-26", isoDateOf(weeks.first().days.first().epochDay))
        assertTrue(weeks.first().days.first().isOutside)
        assertTrue(weeks.flatMap { it.days }.single { it.isToday }.epochDay == today)
        assertEquals(44, weeks.first().number)
    }

    @Test
    fun theServersFirstDayOfTheWeekComesBeforeTheLanguages() = runTest(dispatcher) {
        val viewModel = viewModel()

        val state = viewModel.state.value!!.copy(firstDayOfWeek = 0)

        assertEquals("2026-11-01", isoDateOf(state.weekStartOf(state.anchor)))
    }

    @Test
    fun aTapOnADayShowsThatDay() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.chooseView(CalendarView.MONTH)

        viewModel.openDay(today + 3)
        advanceUntilIdle()

        assertEquals(today + 3, viewModel.state.value!!.anchor)
        assertEquals(CalendarView.DAY, viewModel.state.value!!.view)
    }

    @Test
    fun theColumnsFollowTheView() = runTest(dispatcher) {
        val state = viewModel().state.value!!

        assertEquals(1, state.gridDays(CalendarView.DAY).size)
        val week = state.gridDays(CalendarView.WEEK, today + 7)
        assertEquals(state.weekStartOf(today + 7), week.first().epochDay)
        assertEquals(7, week.size)
    }

    @Test
    fun theSharingLinkIsOnlyKeptWhileItsDialogIsOpen() = runTest(dispatcher) {
        val url = "https://demo.grocy.info/api/calendar/ical?secret=abc"
        environment.server.respond(SHARING_LINK_PATH, body = """{"url":"$url"}""")
        val viewModel = viewModel()

        viewModel.openSharing()
        advanceUntilIdle()
        assertEquals(url, viewModel.sharingLink.value)

        viewModel.closeSharing()
        assertNull(viewModel.sharingLink.value)
    }

    @Test
    fun aSharingLinkThatFailsTellsWhy() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        viewModel.openSharing()
        advanceUntilIdle()

        val event = assertIs<CalendarEvents.SharingFailed>(viewModel.events.first())
        assertEquals(ChangeError.Offline, event.error)
        assertNull(viewModel.sharingLink.value)
    }

    @Test
    fun aColorIsSavedAsGrocysUserSetting() = runTest(dispatcher) {
        val path = "/api/user/settings/calendar_color_tasks"
        environment.server.respond(path, body = "{}", method = HttpMethod.Put)
        val viewModel = viewModel()

        viewModel.saveColor(CalendarColorSetting.TASKS, "#123456")
        advanceUntilIdle()

        assertTrue(environment.server.requestedPaths.contains(path))
    }

    private companion object {
        const val SHARING_LINK_PATH = "/api/calendar/ical/sharing-link"
    }
}
