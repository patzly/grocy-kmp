package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.settings.BatteriesJournalOptionsRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesOverviewOptionsRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteryDueStatus
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class BatteriesViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val now = "2026-11-04 12:00:00"
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

    private fun TestScope.overviewViewModel(): BatteriesOverviewViewModel {
        val viewModel = BatteriesOverviewViewModel(
            masterData = environment.repository,
            batteries = environment.batteriesRepository,
            optionsRepository = BatteriesOverviewOptionsRepository(
                settingsStore,
                backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
            now = { now },
        )
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private fun trackingViewModel() = BatteryTrackingViewModel(
        presetBatteryId = null,
        masterData = environment.repository,
        batteries = environment.batteriesRepository,
        now = { now },
    )

    private fun TestScope.journalViewModel(batteryId: Int? = null): BatteriesJournalViewModel =
        BatteriesJournalViewModel(
            batteryId = batteryId,
            masterData = environment.repository,
            batteries = environment.batteriesRepository,
            optionsRepository = BatteriesJournalOptionsRepository(
                settingsStore,
                backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
            today = { 20_396 },
        )

    @Test
    fun theOverviewCountsLikeGrocysMessagesAndFilters() = runTest(dispatcher) {
        val viewModel = overviewViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(2, state.summary.count)
        assertEquals(1, state.summary.overdue)
        assertEquals(listOf("Remote", "Spare"), state.groups.single().rows.map { it.battery.name })

        viewModel.setStatus(BatteryDueStatus.OVERDUE)
        advanceUntilIdle()
        assertEquals(
            listOf("Remote"),
            viewModel.state.value!!.groups.single().rows.map { it.battery.name },
        )

        viewModel.clearFilter()
        advanceUntilIdle()
        assertNull(viewModel.state.value!!.status)
    }

    @Test
    fun tableOptionsAreKept() = runTest(dispatcher) {
        val viewModel = overviewViewModel()
        advanceUntilIdle()

        viewModel.setGrouping(TableColumn.Own(BatteriesOverviewGrouping.USED_IN))
        advanceUntilIdle()

        assertEquals(
            BatteriesOverviewGrouping.USED_IN,
            settingsStore.batteriesOverviewOptions.value.grouping,
        )
    }

    @Test
    fun theRowsButtonTracksNowAndItsMessageUndoes() = runTest(dispatcher) {
        val viewModel = overviewViewModel()
        advanceUntilIdle()

        viewModel.actions.events.test {
            viewModel.actions.track(1, "Remote")
            advanceUntilIdle()

            val tracked = assertIs<BatteryEvent.Tracked>(awaitItem())
            assertEquals(now, tracked.trackedTime)
            assertEquals(23, tracked.cycleId)

            viewModel.actions.undo(tracked)
            advanceUntilIdle()

            assertEquals(BatteryEvent.Undone, awaitItem())
        }
        assertEquals(
            1,
            environment.server
                .requestsTo("/api/batteries/charge-cycles/23/undo", HttpMethod.Post).size,
        )
    }

    @Test
    fun aSecondTapWaitsForTheFirst() = runTest(dispatcher) {
        val viewModel = overviewViewModel()
        advanceUntilIdle()

        viewModel.actions.track(1, "Remote")
        viewModel.actions.track(1, "Remote")
        advanceUntilIdle()

        assertEquals(1, environment.server.requestsTo("/api/batteries/1/charge", HttpMethod.Post).size)
    }

    @Test
    fun offlineTrackingSaysSo() = runTest(dispatcher) {
        val viewModel = overviewViewModel()
        advanceUntilIdle()
        environment.server.unreachable = true

        viewModel.actions.events.test {
            viewModel.actions.track(1, "Remote")
            advanceUntilIdle()

            assertEquals(
                ChangeError.Offline,
                assertIs<BatteryEvent.Failed>(awaitItem()).error,
            )
        }
    }

    @Test
    fun theTrackingFormNeedsABatteryAndATimeNotAfterNow() = runTest(dispatcher) {
        val viewModel = trackingViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.track())
        assertEquals(
            FieldError.Required,
            viewModel.errors[BatteryTrackingField.BATTERY],
        )

        viewModel.selectBattery(1)
        viewModel.trackedTime.setTextAndPlaceCursorAtEnd("2026-11-04 12:00:01")
        assertFalse(viewModel.track())
        assertEquals(
            FieldError.NotBeforeNow,
            viewModel.errors[BatteryTrackingField.TRACKED_TIME],
        )

        viewModel.trackedTime.setTextAndPlaceCursorAtEnd("no date")
        assertFalse(viewModel.track())
        assertEquals(
            FieldError.InvalidDateTime,
            viewModel.errors[BatteryTrackingField.TRACKED_TIME],
        )
    }

    @Test
    fun aTrackedBatteryStartsTheFormOverAndStaysShownForItsOverview() = runTest(dispatcher) {
        val viewModel = trackingViewModel()
        backgroundScope.launch { viewModel.shownBattery.collect {} }
        advanceUntilIdle()
        viewModel.selectBattery(1)
        viewModel.trackedTime.setTextAndPlaceCursorAtEnd("2026-11-04 10:00:00")

        viewModel.events.test {
            assertTrue(viewModel.track())
            advanceUntilIdle()

            val result = assertIs<BatteryTrackingEvent.Result>(awaitItem())
            val tracked = assertIs<BatteryEvent.Tracked>(result.event)
            assertEquals("2026-11-04 10:00:00", tracked.trackedTime)
        }
        assertNull(viewModel.batteryId)
        assertEquals(now, viewModel.trackedTime.text.toString())
        assertEquals(1, viewModel.shownBattery.value?.id)
    }

    @Test
    fun scanningAGrocycodeChoosesItsBattery() = runTest(dispatcher) {
        val viewModel = trackingViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.onScanned("grcy:b:1"))
        assertEquals(1, viewModel.batteryId)

        viewModel.events.test {
            assertFalse(viewModel.onScanned("grcy:c:1"))
            advanceUntilIdle()
            assertEquals(BatteryTrackingEvent.UnknownCode, awaitItem())
        }
    }

    @Test
    fun aPresetBatteryIsChosenAtOnce() = runTest(dispatcher) {
        val viewModel = BatteryTrackingViewModel(
            presetBatteryId = 1,
            masterData = environment.repository,
            batteries = environment.batteriesRepository,
            now = { now },
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.batteryId)
    }

    @Test
    fun theOverviewDialogShowsCardAndCounts() = runTest(dispatcher) {
        val overview = BatteryOverviewViewModel(
            environment.repository,
            environment.batteriesRepository,
            now = { now },
        )
        backgroundScope.launch { overview.state.collect {} }

        overview.show(1, "Remote")
        advanceUntilIdle()

        val state = overview.state.value!!
        assertEquals("Remote", state.card?.name)
        assertEquals("Living room", state.card?.usedIn)
        assertEquals(4, state.card?.chargeCyclesCount)
        assertEquals("2026-10-01 08:00:00", state.card?.lastCharged)
        assertEquals("2026-10-31 08:00:00", state.card?.next)
        assertTrue(state.hasDetails)

        overview.hide()
        advanceUntilIdle()
        assertNull(overview.shownBattery.value)
    }

    @Test
    fun theJournalStartsWithItsBatteryAndUndoes() = runTest(dispatcher) {
        environment.batteriesRepository.refresh()
        val viewModel = journalViewModel(batteryId = 1)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(1, state.filter.batteryId)
        assertEquals(listOf(22, 21), state.groups.single().rows.map { it.cycle.id })

        viewModel.events.test {
            viewModel.undo(state.groups.single().rows.first().cycle)
            advanceUntilIdle()
            assertEquals(BatteriesJournalEvent.Undone, awaitItem())
        }
        assertEquals(
            1,
            environment.server
                .requestsTo("/api/batteries/charge-cycles/22/undo", HttpMethod.Post).size,
        )
    }

    @Test
    fun anUndoneCycleCannotBeUndoneAgain() = runTest(dispatcher) {
        environment.batteriesRepository.refresh()
        val viewModel = journalViewModel()
        advanceUntilIdle()
        val undone = viewModel.state.value!!.groups.single().rows.last().cycle

        viewModel.undo(undone)
        advanceUntilIdle()

        assertTrue(
            environment.server
                .requestsTo("/api/batteries/charge-cycles/21/undo", HttpMethod.Post).isEmpty(),
        )
    }

    @Test
    fun aLongerRangeDownloadsAgainAndTheJournalKeepsItsTableOptions() = runTest(dispatcher) {
        val viewModel = journalViewModel()
        advanceUntilIdle()
        val before = environment.server
            .requestsTo("/api/objects/battery_charge_cycles", HttpMethod.Get).size

        viewModel.setFilter { it.copy(months = BATTERIES_JOURNAL_ALL_MONTHS) }
        viewModel.setGrouping(BatteriesJournalGrouping.BATTERY)
        advanceUntilIdle()

        assertEquals(
            before + 1,
            environment.server
                .requestsTo("/api/objects/battery_charge_cycles", HttpMethod.Get).size,
        )
        assertEquals(
            BatteriesJournalGrouping.BATTERY,
            settingsStore.batteriesJournalOptions.value.grouping,
        )
    }
}
