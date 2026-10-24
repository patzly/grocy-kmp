package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ObjectFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = MasterDataTestEnvironment(dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.refreshed() {
        environment.repository.refresh()
        advanceUntilIdle()
    }

    private val <F : ObjectFormState<*>> ObjectFormViewModel<*, F>.form: F
        get() = assertIs<ObjectFormLoad.Ready<F>>(load).form

    @Test
    fun existingBatteryIsLoadedFromTheDatabase() = runTest(dispatcher) {
        refreshed()

        val viewModel = SimpleObjectViewModel(ObjectListKind.BATTERIES, 1, environment.repository)
        advanceUntilIdle()

        assertEquals("Remote", viewModel.form.name.text.toString())
        assertEquals("30", viewModel.form.chargeIntervalDays.text.toString())
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun unknownObjectIsNotFound() = runTest(dispatcher) {
        val viewModel = SimpleObjectViewModel(ObjectListKind.STORES, 42, environment.repository)
        advanceUntilIdle()

        assertEquals(ObjectFormLoad.NotFound, viewModel.load)
    }

    @Test
    fun objectOfALinkIsDownloadedFirst() = runTest(dispatcher) {
        val viewModel = SimpleObjectViewModel(ObjectListKind.STORES, 1, environment.repository)
        advanceUntilIdle()

        assertEquals("Supermarket", viewModel.form.name.text.toString())
    }

    @Test
    fun invalidInputIsNotSent() = runTest(dispatcher) {
        refreshed()
        val viewModel = SimpleObjectViewModel(ObjectListKind.LOCATIONS, 1, environment.repository)
        advanceUntilIdle()
        viewModel.form.name.setTextAndPlaceCursorAtEnd(" ")

        val sent = viewModel.save()

        assertFalse(sent)
        assertEquals(setOf(ObjectField.NAME), viewModel.errors.keys)
        assertTrue(viewModel.hasUnsavedChanges)
        val puts = environment.server.requestsTo("/api/objects/locations/1", HttpMethod.Put)
        assertTrue(puts.isEmpty())
    }

    @Test
    fun savedLocationClosesTheForm() = runTest(dispatcher) {
        refreshed()
        environment.server.respond(
            "/api/objects/locations/1",
            body = """{"id":1,"name":"Fridge","description":"Cold"}""",
        )
        val viewModel = SimpleObjectViewModel(ObjectListKind.LOCATIONS, 1, environment.repository)
        advanceUntilIdle()
        viewModel.form.description.setTextAndPlaceCursorAtEnd("Cold")

        viewModel.events.test {
            assertTrue(viewModel.save())
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
        val put = environment.server.requestsTo("/api/objects/locations/1", HttpMethod.Put)
        assertEquals(1, put.size)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun createdObjectTurnsIntoItsEditForm() = runTest(dispatcher) {
        refreshed()
        environment.server.respond(
            "/api/objects/task_categories",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        environment.server.respond(
            "/api/objects/task_categories/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"name":"Kitchen"}""",
        )
        val viewModel =
            SimpleObjectViewModel(ObjectListKind.TASK_CATEGORIES, null, environment.repository)
        advanceUntilIdle()
        viewModel.form.name.setTextAndPlaceCursorAtEnd("Kitchen")

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Created, awaitItem())
        }
        assertEquals(MasterDataJson.CREATED_ID, viewModel.objectId)
        assertFalse(viewModel.isNew)
    }

    @Test
    fun offlineSaveKeepsTheChanges() = runTest(dispatcher) {
        refreshed()
        val viewModel = SimpleObjectViewModel(ObjectListKind.STORES, 1, environment.repository)
        advanceUntilIdle()
        viewModel.form.name.setTextAndPlaceCursorAtEnd("Market")
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Offline, awaitItem())
        }
        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun changedUserfieldsCountAsUnsavedChanges() = runTest(dispatcher) {
        refreshed()
        val viewModel = ChoreViewModel(1, environment.repository, now = { NOW })
        advanceUntilIdle()
        val ready = assertIs<ObjectFormLoad.Ready<ChoreFormState>>(viewModel.load)
        val room = ready.userfields.single()
        assertEquals("Living room", room.text.text.toString())

        room.text.setTextAndPlaceCursorAtEnd("Hall")

        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun newChoreStartsNowWithGrocysFirstChoices() = runTest(dispatcher) {
        refreshed()

        val viewModel = ChoreViewModel(null, environment.repository, now = { NOW })
        advanceUntilIdle()

        val values = viewModel.form.values()
        assertEquals(NOW, values.startDate)
        assertEquals(ChorePeriodType.HOURLY, values.periodType)
        assertEquals(ChoreAssignmentType.IN_ALPHABETICAL_ORDER, values.assignmentType)
        assertEquals(listOf("Alice", "Bob"), viewModel.options.value.users.map { it.displayName })
    }

    @Test
    fun savedChoreSendsItsScheduleAndUserfields() = runTest(dispatcher) {
        refreshed()
        environment.server.respond(
            "/api/objects/chores/1",
            body = """{"id":1,"name":"Vacuum","period_type":"weekly","period_interval":3}""",
        )
        val viewModel = ChoreViewModel(1, environment.repository, now = { NOW })
        advanceUntilIdle()
        viewModel.form.periodInterval.setTextAndPlaceCursorAtEnd("3")
        assertIs<ObjectFormLoad.Ready<ChoreFormState>>(viewModel.load).userfields.single()
            .text.setTextAndPlaceCursorAtEnd("Hall")

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
        val put = environment.server.requestsTo("/api/objects/chores/1", HttpMethod.Put).single()
        val body = (put.body as TextContent).text
        assertTrue(""""period_interval":3""" in body, body)
        assertTrue(""""period_config":"monday,friday"""" in body, body)
        val userfields = environment.server.requestsTo("/api/userfields/chores/1", HttpMethod.Put)
        assertEquals(1, userfields.size)
    }

    @Test
    fun newUserfieldBelongsToTheEntityOfItsList() = runTest(dispatcher) {
        refreshed()

        val viewModel = UserfieldViewModel(null, "batteries", environment.repository)
        advanceUntilIdle()

        assertEquals("batteries", viewModel.form.entity)
        assertEquals(null, viewModel.form.type)
        assertFalse(viewModel.save())
        assertEquals(
            setOf(ObjectField.NAME, ObjectField.CAPTION, ObjectField.TYPE),
            viewModel.errors.keys,
        )
    }

    @Test
    fun userentityKnowsItsSavedNameForItsFields() = runTest(dispatcher) {
        refreshed()

        val viewModel = UserentityViewModel(1, environment.repository)
        advanceUntilIdle()
        viewModel.form.name.setTextAndPlaceCursorAtEnd("magazines")

        assertEquals("books", viewModel.savedName)
    }

    private companion object {
        const val NOW = "2026-10-24 12:00:00"
    }
}
