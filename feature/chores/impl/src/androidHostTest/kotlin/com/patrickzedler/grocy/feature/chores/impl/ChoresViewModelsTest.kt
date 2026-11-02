package com.patrickzedler.grocy.feature.chores.impl

import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.settings.ChoresJournalOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ChoresOverviewOptionsRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.ScannerFieldState
import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewField
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalField
import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalSortField
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChoresViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val now = "2026-11-02 12:00:00"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun executeBodies(chore: Int = 1) =
        environment.server.requestsTo("/api/chores/$chore/execute", HttpMethod.Post).map {
            Json.parseToJsonElement((it.body as TextContent).text).jsonObject
        }

    private suspend fun TestScope.overview(
        options: ChoresOverviewOptions = ChoresOverviewOptions(),
    ): ChoresOverviewViewModel {
        environment.repository.refresh()
        return ChoresOverviewViewModel(
            masterData = environment.repository,
            chores = environment.choresRepository,
            optionsRepository = ChoresOverviewOptionsRepository(
                FakeSettingsStore(choresOverviewOptions = options),
                backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
            now = { now },
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    private fun TestScope.journalOptions(
        options: ChoresJournalOptions = ChoresJournalOptions(),
    ) = ChoresJournalOptionsRepository(
        FakeSettingsStore(choresJournalOptions = options),
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    private suspend fun TestScope.journal(
        options: ChoresJournalOptions = ChoresJournalOptions(),
    ): ChoresJournalViewModel {
        environment.repository.refresh()
        environment.server.respond(
            "/api/objects/chores_log",
            body = """[
                {"id":13,"chore_id":1,"tracked_time":"2026-11-02 08:00:00"},
                {"id":14,"chore_id":2,"tracked_time":"2026-11-01 08:00:00"}
            ]""",
        )
        return ChoresJournalViewModel(
            choreId = null,
            masterData = environment.repository,
            chores = environment.choresRepository,
            optionsRepository = journalOptions(options),
            today = { epochDayOf("2026-11-02")!! },
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun journalHidesTheTimeOfTrackingUntilItIsChosen() = runTest(dispatcher) {
        val viewModel = journal()

        assertFalse(ChoresJournalField.TIME_OF_TRACKING in viewModel.state.value!!.fields)
        assertTrue(ChoresJournalField.TIME_OF_TRACKING in viewModel.state.value!!.availableFields)

        viewModel.setFieldShown(ChoresJournalField.TIME_OF_TRACKING, true)
        advanceUntilIdle()

        assertTrue(ChoresJournalField.TIME_OF_TRACKING in viewModel.state.value!!.fields)
    }

    @Test
    fun journalGroupsByTheChosenColumn() = runTest(dispatcher) {
        val viewModel = journal()
        assertEquals(1, viewModel.state.value!!.groups.size)

        viewModel.setGrouping(ChoresJournalGrouping.CHORE)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(ChoresJournalGrouping.CHORE, state.grouping)
        assertEquals(2, state.groups.size)
        assertTrue(state.groups.all { it.title != null })
    }

    @Test
    fun journalSortsByTheChosenColumn() = runTest(dispatcher) {
        val viewModel = journal()
        assertTrue(viewModel.state.value!!.isDefaultSort)
        assertEquals(
            listOf(13, 14),
            viewModel.state.value!!.groups.single().rows.map { it.execution.id },
        )

        viewModel.setSort(ChoresJournalSortField.TRACKED_TIME, ascending = true)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertFalse(state.isDefaultSort)
        assertEquals(listOf(14, 13), state.groups.single().rows.map { it.execution.id })
    }

    private suspend fun TestScope.tracking(
        presetChoreId: Int? = null,
        scanner: ScannerFieldState? = null,
    ): ChoreTrackingViewModel {
        environment.repository.refresh()
        return ChoreTrackingViewModel(
            presetChoreId = presetChoreId,
            masterData = environment.repository,
            chores = environment.choresRepository,
            scanner = { hasValue -> scanner ?: ScannerFieldState(hasValue = hasValue) },
            now = { now },
        ).also {
            backgroundScope.launch { it.shownChore.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun overviewShowsTheDownloadedChores() = runTest(dispatcher) {
        val state = overview().state.value!!

        assertEquals(
            listOf("Vacuum", "Water plants"),
            state.groups.single().rows.map { it.chore.name },
        )
        assertEquals(ChoreDueStatus.OVERDUE, state.groups.single().rows.first().dueStatus)
        assertEquals(1, state.summary.overdue)
        assertEquals(1, state.summary.assignedToMe)
        assertEquals(1, state.currentUserId)
        assertTrue(state.hasChores)
    }

    @Test
    fun overviewKeepsItsTableOptions() = runTest(dispatcher) {
        val state = overview(
            ChoresOverviewOptions(
                fields = setOf(ChoresOverviewField.LAST_TRACKED),
                grouping = ChoresOverviewGrouping.ASSIGNED_TO,
            ),
        ).state.value!!

        assertEquals(listOf(ChoresOverviewField.LAST_TRACKED), state.fields)
        assertEquals(listOf("Alice", null), state.groups.map { it.title })
    }

    @Test
    fun clearFilterKeepsTheTableOptions() = runTest(dispatcher) {
        val viewModel =
            overview(ChoresOverviewOptions(grouping = ChoresOverviewGrouping.ASSIGNED_TO))
        viewModel.setFilter { it.copy(status = ChoreDueStatus.OVERDUE) }
        viewModel.showSearch()
        advanceUntilIdle()

        viewModel.clearFilter()
        advanceUntilIdle()

        assertFalse(viewModel.state.value!!.filter.isActive)
        assertFalse(viewModel.isSearchVisible)
        assertEquals(
            TableColumn.Own(ChoresOverviewGrouping.ASSIGNED_TO),
            viewModel.state.value!!.grouping,
        )
    }

    @Test
    fun trackingFromTheOverviewTracksTheNextSchedule() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore

        viewModel.actions.events.test {
            viewModel.actions.track(vacuum, atNextSchedule = true)
            advanceUntilIdle()

            val event = assertIs<ChoreEvent.Tracked>(awaitItem())
            assertEquals("2026-11-02 08:00:00", event.trackedTime)
            assertEquals(13, event.executionId)
        }
        val body = executeBodies().single()
        assertEquals("2026-11-02 08:00:00", body["tracked_time"]!!.jsonPrimitive.content)
        assertFalse(body["skipped"]!!.jsonPrimitive.boolean)
        assertFalse("done_by" in body)
    }

    @Test
    fun trackingNowTracksNow() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore

        viewModel.actions.track(vacuum, atNextSchedule = false)
        advanceUntilIdle()

        assertEquals(now, executeBodies().single()["tracked_time"]!!.jsonPrimitive.content)
    }

    @Test
    fun skippingTracksTheNextScheduleAsSkipped() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore

        viewModel.actions.skip(vacuum)
        advanceUntilIdle()

        val body = executeBodies().single()
        assertEquals("2026-11-02 08:00:00", body["tracked_time"]!!.jsonPrimitive.content)
        assertTrue(body["skipped"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun aSecondTapWaitsForTheFirst() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore

        viewModel.actions.track(vacuum, atNextSchedule = true)
        viewModel.actions.track(vacuum, atNextSchedule = true)
        advanceUntilIdle()

        assertEquals(1, executeBodies().size)
    }

    @Test
    fun undoFromTheMessageUndoesTheExecution() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore

        viewModel.actions.events.test {
            viewModel.actions.track(vacuum, atNextSchedule = true)
            advanceUntilIdle()
            viewModel.actions.undo(assertIs<ChoreEvent.Tracked>(awaitItem()))
            advanceUntilIdle()

            assertEquals(ChoreEvent.Undone, awaitItem())
        }
        assertEquals(
            1,
            environment.server.requestsTo("/api/chores/executions/13/undo", HttpMethod.Post).size,
        )
    }

    @Test
    fun trackingOfflineTellsSo() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore
        environment.server.unreachable = true

        viewModel.actions.events.test {
            viewModel.actions.track(vacuum, atNextSchedule = true)
            advanceUntilIdle()

            assertEquals(ChoreEvent.Failed(ChangeError.Offline), awaitItem())
        }
    }

    @Test
    fun rescheduleDialogStartsWithTheNextExecutionAndItsUser() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore

        viewModel.actions.openReschedule(vacuum)

        val form = viewModel.actions.reschedule!!
        assertEquals("2026-11-02 08:00:00", form.date.text.toString())
        assertEquals(1, form.assignedUserId)
        assertTrue(form.canReset)
    }

    @Test
    fun rescheduleDialogOfADateOnlyChoreWithoutScheduleStartsToday() = runTest(dispatcher) {
        val viewModel = overview()
        val water = viewModel.state.value!!.groups.single().rows.last().chore

        viewModel.actions.openReschedule(water)

        val form = viewModel.actions.reschedule!!
        assertEquals("2026-11-02", form.date.text.toString())
        assertFalse(form.canReset)
    }

    @Test
    fun rescheduleNeedsADate() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore
        viewModel.actions.openReschedule(vacuum)
        val form = viewModel.actions.reschedule!!
        form.date.edit { replace(0, length, "") }

        viewModel.actions.saveReschedule()
        advanceUntilIdle()

        assertEquals(FieldError.Required, form.dateError)
        assertNotNull(viewModel.actions.reschedule)
        assertTrue(
            environment.server.requestsTo("/api/objects/chores/1", HttpMethod.Put).isEmpty(),
        )
    }

    @Test
    fun rescheduleSavesAndCloses() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore
        viewModel.actions.openReschedule(vacuum)

        viewModel.actions.saveReschedule()
        advanceUntilIdle()

        assertNull(viewModel.actions.reschedule)
        assertEquals(
            1,
            environment.server.requestsTo("/api/objects/chores/1", HttpMethod.Put).size,
        )
    }

    @Test
    fun aFailedRescheduleStaysOpenAndSaysWhy() = runTest(dispatcher) {
        val viewModel = overview()
        val vacuum = viewModel.state.value!!.groups.single().rows.first().chore
        viewModel.actions.openReschedule(vacuum)
        environment.server.unreachable = true

        viewModel.actions.resetReschedule()
        advanceUntilIdle()

        assertEquals(ChangeError.Offline, viewModel.actions.reschedule?.error)
    }

    private suspend fun TestScope.choreOverview(): ChoreOverviewViewModel {
        environment.repository.refresh()
        return ChoreOverviewViewModel(
            masterData = environment.repository,
            chores = environment.choresRepository,
            now = { now },
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun choreOverviewIsClosedUntilAChoreIsShown() = runTest(dispatcher) {
        val viewModel = choreOverview()

        assertNull(viewModel.shownChore.value)
        assertNull(viewModel.state.value)
        assertTrue(
            environment.server.requestsTo("/api/chores/1", HttpMethod.Get).isEmpty(),
        )
    }

    @Test
    fun choreOverviewShowsWhatTheServerComputed() = runTest(dispatcher) {
        val viewModel = choreOverview()

        viewModel.show(1, "Vacuum")
        assertEquals(ShownChore(1, "Vacuum"), viewModel.shownChore.value)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        val card = state.card!!
        assertEquals(1, state.choreId)
        assertEquals("Vacuum", card.name)
        assertEquals("Living room", card.description)
        assertEquals(4, card.trackedCount)
        assertEquals(72.5, card.averageFrequencyHours)
        assertEquals("Alice", card.lastDoneByName)
        assertEquals("Alice", card.assignedUserName)
        assertTrue(card.isRescheduled)
        assertTrue(state.canSkip)
        assertTrue(state.hasDetails)
    }

    @Test
    fun choreOverviewDownloadsEveryTimeItOpens() = runTest(dispatcher) {
        val viewModel = choreOverview()

        viewModel.show(1, "Vacuum")
        advanceUntilIdle()
        viewModel.hide()
        viewModel.show(1, "Vacuum")
        advanceUntilIdle()

        assertEquals(
            2,
            environment.server.requestsTo("/api/chores/1", HttpMethod.Get).size,
        )
    }

    @Test
    fun closingTheChoreOverviewEmptiesIt() = runTest(dispatcher) {
        val viewModel = choreOverview()
        viewModel.show(1, "Vacuum")
        advanceUntilIdle()

        viewModel.hide()
        advanceUntilIdle()

        assertNull(viewModel.shownChore.value)
        assertNull(viewModel.state.value)
    }

    @Test
    fun journalShowsItsRangeAndUndoes() = runTest(dispatcher) {
        environment.repository.refresh()
        environment.server.respond(
            "/api/objects/chores_log",
            body = """[{"id":13,"chore_id":1,"tracked_time":"2026-11-02 08:00:00"}]""",
        )
        val viewModel = ChoresJournalViewModel(
            choreId = 1,
            masterData = environment.repository,
            chores = environment.choresRepository,
            optionsRepository = journalOptions(),
            today = { epochDayOf("2026-11-02")!! },
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        val request = environment.server
            .requestsTo("/api/objects/chores_log", HttpMethod.Get).single()
        assertEquals(listOf("tracked_time>=2025-11-02"), request.url.parameters.getAll("query[]"))
        assertEquals(
            listOf(13),
            viewModel.state.value!!.groups.single().rows.map { it.execution.id },
        )

        viewModel.events.test {
            viewModel.undo(viewModel.state.value!!.groups.single().rows.single().execution)
            advanceUntilIdle()
            assertEquals(ChoresJournalEvent.Undone, awaitItem())
        }
    }

    @Test
    fun aLongerRangeDownloadsAgain() = runTest(dispatcher) {
        environment.repository.refresh()
        val viewModel = ChoresJournalViewModel(
            choreId = null,
            masterData = environment.repository,
            chores = environment.choresRepository,
            optionsRepository = journalOptions(),
            today = { epochDayOf("2026-11-02")!! },
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.setFilter { it.copy(months = CHORES_JOURNAL_ALL_MONTHS) }
        advanceUntilIdle()

        val requests = environment.server.requestsTo("/api/objects/chores_log", HttpMethod.Get)
        assertEquals(2, requests.size)
        assertNull(requests.last().url.parameters.getAll("query[]"))
    }

    @Test
    fun trackingFormStartsWithTheFieldForKeyboardUsers() = runTest(dispatcher) {
        val viewModel = tracking()

        assertFalse(viewModel.scanner.isOpen)
        assertNull(viewModel.choreId)
        assertEquals(now, viewModel.trackedTime.text.toString())
    }

    @Test
    fun trackingFormPrefillsTheLoggedInUserOnceKnown() = runTest(dispatcher) {
        val viewModel = tracking()

        assertEquals(1, viewModel.doneByUserId)
    }

    @Test
    fun trackingOffersOnlyActiveChores() = runTest(dispatcher) {
        val viewModel = tracking()

        assertEquals(listOf("Vacuum"), viewModel.options.value!!.chores.map { it.name })
    }

    @Test
    fun presetChoreIsChosenWithoutScannerOrKeyboard() = runTest(dispatcher) {
        val viewModel = tracking(presetChoreId = 1)

        assertEquals(1, viewModel.choreId)
        assertFalse(viewModel.scanner.isOpen)
        assertNull(viewModel.scanner.focusTarget)
        assertEquals(ShownChore(1, "Vacuum"), viewModel.shownChore.value)
    }

    @Test
    fun trackingWithoutChoreSaysSoAndSendsNothing() = runTest(dispatcher) {
        val viewModel = tracking()

        assertFalse(viewModel.track())
        advanceUntilIdle()

        assertEquals(FieldError.Required, viewModel.errors[ChoreTrackingField.CHORE])
        assertTrue(executeBodies().isEmpty())
    }

    @Test
    fun anInvalidTimeSaysSo() = runTest(dispatcher) {
        val viewModel = tracking(presetChoreId = 1)
        viewModel.trackedTime.edit { replace(0, length, "yesterday") }

        assertFalse(viewModel.track())

        assertEquals(FieldError.InvalidDateTime, viewModel.errors[ChoreTrackingField.TRACKED_TIME])
    }

    @Test
    fun trackingSendsTheFormAndStartsOver() = runTest(dispatcher) {
        val viewModel = tracking()
        viewModel.selectChore(1)
        advanceUntilIdle()

        viewModel.events.test {
            assertTrue(viewModel.track())
            advanceUntilIdle()

            val result = assertIs<ChoreTrackingEvent.Result>(awaitItem())
            assertIs<ChoreEvent.Tracked>(result.event)
        }
        val body = executeBodies().single()
        assertEquals(now, body["tracked_time"]!!.jsonPrimitive.content)
        assertEquals(1, body["done_by"]!!.jsonPrimitive.int)
        assertNull(viewModel.choreId)
        // Its overview stays available for the chore just tracked
        assertEquals(ShownChore(1, "Vacuum"), viewModel.shownChore.value)
    }

    @Test
    fun skipSendsTheFormAsSkipped() = runTest(dispatcher) {
        val viewModel = tracking(presetChoreId = 1)

        viewModel.track(skipped = true)
        advanceUntilIdle()

        assertTrue(executeBodies().single()["skipped"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun aFailedTrackingKeepsTheForm() = runTest(dispatcher) {
        val viewModel = tracking(presetChoreId = 1)
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.track()
            advanceUntilIdle()

            assertEquals(
                ChoreTrackingEvent.Result(ChoreEvent.Failed(ChangeError.Offline)),
                awaitItem(),
            )
        }
        assertEquals(1, viewModel.choreId)
    }

    @Test
    fun aScannedGrocycodeChoosesTheChoreWithoutKeyboard() = runTest(dispatcher) {
        val scanner = ScannerFieldState(rememberedOpen = true)
        val viewModel = tracking(scanner = scanner)

        assertTrue(viewModel.onScanned("grcy:c:1"))

        assertEquals(1, viewModel.choreId)
        assertFalse(scanner.isOpen)
        assertNull(scanner.focusTarget)
    }

    @Test
    fun aScannedUnknownCodeKeepsScanningAndSaysSo() = runTest(dispatcher) {
        val scanner = ScannerFieldState(rememberedOpen = true)
        val viewModel = tracking(scanner = scanner)

        viewModel.events.test {
            assertFalse(viewModel.onScanned("4006381333931"))
            assertFalse(viewModel.onScanned("grcy:c:2"))
            advanceUntilIdle()

            assertEquals(ChoreTrackingEvent.UnknownCode, awaitItem())
            assertEquals(ChoreTrackingEvent.UnknownCode, awaitItem())
        }
        assertTrue(scanner.isOpen)
        assertNull(viewModel.choreId)
    }

    @Test
    fun theNextChoreIsScannedAgain() = runTest(dispatcher) {
        val scanner = ScannerFieldState(rememberedOpen = true)
        val viewModel = tracking(scanner = scanner)
        viewModel.onScanned("grcy:c:1")

        viewModel.track()
        advanceUntilIdle()

        assertTrue(scanner.isOpen)
    }

    @Test
    fun aCodeTypedByABarcodeReaderMovesOnToTheTime() = runTest(dispatcher) {
        val viewModel = tracking()
        viewModel.scanner.onFocusHandled()

        assertTrue(viewModel.onTextSubmitted("grcy:c:1"))

        assertEquals(1, viewModel.choreId)
        assertEquals(
            FormFocusTarget.NEXT_FIELD,
            viewModel.scanner.focusTarget,
        )
    }

    @Test
    fun plainTextIsNoCode() = runTest(dispatcher) {
        val viewModel = tracking()

        assertFalse(viewModel.onTextSubmitted("Vacuum"))
    }

    @Test
    fun theKeyboardStaysInTheFieldWithoutAChore() = runTest(dispatcher) {
        val viewModel = tracking()
        viewModel.scanner.onFocusHandled()

        viewModel.goToTrackedTime()
        assertNull(viewModel.scanner.focusTarget)

        viewModel.selectChore(1)
        viewModel.goToTrackedTime()
        assertEquals(
            FormFocusTarget.NEXT_FIELD,
            viewModel.scanner.focusTarget,
        )
    }
}
