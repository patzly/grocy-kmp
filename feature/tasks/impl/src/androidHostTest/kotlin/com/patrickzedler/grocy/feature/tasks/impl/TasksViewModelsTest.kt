package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.settings.TasksOverviewOptionsRepository
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormEvent
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.tasks.TaskDueStatus
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TasksViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val now = "2026-11-02 12:00:00"
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

    private suspend fun TestScope.list(): TasksViewModel {
        environment.repository.refresh()
        val viewModel = TasksViewModel(
            environment.repository,
            environment.tasksRepository,
            TasksOverviewOptionsRepository(
                settingsStore,
                backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
            now = { now },
            today = { epochDayOf("2026-11-02")!! },
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private suspend fun TestScope.form(id: Int? = null): TaskFormViewModel {
        environment.repository.refresh()
        environment.tasksRepository.refresh()
        val viewModel = TaskFormViewModel(id, environment.repository, environment.tasksRepository)
        advanceUntilIdle()
        return viewModel
    }

    private fun TaskFormViewModel.form(): TaskFormState =
        assertIs<ObjectFormLoad.Ready<TaskFormState>>(load).form

    private fun TasksUiState.names() = groups.flatMap { it.rows }.map { it.task.name }

    @Test
    fun theListDownloadsAndShowsTheTasksStillToDo() = runTest(dispatcher) {
        val viewModel = list()

        val state = viewModel.state.value!!
        assertEquals(listOf("Mow the lawn", "Buy seeds"), state.names())
        assertEquals(2, state.summary.count)
        assertEquals(1, state.summary.overdue)
        assertTrue(state.hasTasks)
    }

    @Test
    fun showDoneTasksAddsThemAndClearingFiltersTakesThemAway() = runTest(dispatcher) {
        val viewModel = list()

        viewModel.setFilter { it.copy(showDone = true) }
        advanceUntilIdle()
        assertTrue("Old task" in viewModel.state.value!!.names())

        viewModel.clearFilter()
        advanceUntilIdle()
        assertFalse("Old task" in viewModel.state.value!!.names())
    }

    @Test
    fun statusFilterNarrowsTheList() = runTest(dispatcher) {
        val viewModel = list()

        viewModel.setFilter { it.copy(status = TaskDueStatus.OVERDUE) }
        advanceUntilIdle()

        assertEquals(listOf("Mow the lawn"), viewModel.state.value!!.names())
    }

    @Test
    fun tableOptionsAreKept() = runTest(dispatcher) {
        val viewModel = list()

        viewModel.setGrouping(TableColumn.Own(TasksOverviewGrouping.CATEGORY))
        advanceUntilIdle()

        assertEquals(
            TasksOverviewOptions(grouping = TasksOverviewGrouping.CATEGORY),
            settingsStore.tasksOverviewOptions.value,
        )
        assertEquals(
            TableColumn.Own(TasksOverviewGrouping.CATEGORY),
            viewModel.state.value!!.grouping,
        )
    }

    @Test
    fun completingTellsWhatWasDoneAndUndoingBringsItBack() = runTest(dispatcher) {
        val viewModel = list()
        val mow = viewModel.state.value!!.groups.single().rows.first().task
        environment.server.respond(
            "/api/objects/tasks/1",
            body = MasterDataJson.COMPLETED_TASK,
        )

        viewModel.actions.events.test {
            viewModel.actions.complete(mow)
            advanceUntilIdle()

            assertEquals(TaskEvent.Completed(1, "Mow the lawn", now), awaitItem())
            assertEquals(listOf("Buy seeds"), viewModel.state.value!!.names())

            viewModel.actions.undo(1)
            advanceUntilIdle()
            expectNoEvents()
        }
        val completed = environment.server.requestsTo("/api/tasks/1/complete", HttpMethod.Post)
        val body = Json.parseToJsonElement((completed.single().body as TextContent).text)
        assertEquals(now, body.jsonObject.getValue("done_time").jsonPrimitive.content)
        assertEquals(1, environment.server.requestsTo("/api/tasks/1/undo", HttpMethod.Post).size)
    }

    @Test
    fun completingOfflineSaysSo() = runTest(dispatcher) {
        val viewModel = list()
        val mow = viewModel.state.value!!.groups.single().rows.first().task
        environment.server.unreachable = true

        viewModel.actions.events.test {
            viewModel.actions.complete(mow)
            advanceUntilIdle()

            assertEquals(TaskEvent.Failed(ChangeError.Offline), awaitItem())
        }
    }

    @Test
    fun aNewTaskIsAssignedToTheLoggedInUserLikeGrocysForm() = runTest(dispatcher) {
        val viewModel = form()

        assertTrue(viewModel.isNew)
        assertEquals(1, viewModel.form().assignedUserId)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun editingShowsTheTask() = runTest(dispatcher) {
        val viewModel = form(1)

        val form = viewModel.form()
        assertEquals("Mow the lawn", form.name.text.toString())
        assertEquals("Mind the roses", form.description.text.toString())
        assertEquals("2026-11-01", form.dueDate.text.toString())
        assertEquals(1, form.categoryId)
        assertEquals(1, form.assignedUserId)
    }

    @Test
    fun aNameIsRequiredAndTheDueDateMustBeOne() = runTest(dispatcher) {
        val viewModel = form()
        viewModel.form().dueDate.setTextAndPlaceCursorAtEnd("soon")

        assertFalse(viewModel.save())

        assertEquals(
            mapOf(
                ObjectField.NAME to FieldError.Required,
                ObjectField.DUE_DATE to FieldError.InvalidDate,
            ),
            viewModel.errors,
        )
    }

    @Test
    fun aTaskNeedsNoDueDate() {
        assertEquals(emptyMap(), TaskValues(name = "Mow").validate())
        assertNull(TaskValues(name = "Mow").toTask(1).dueDate)
    }

    @Test
    fun savingANewTaskClosesTheForm() = runTest(dispatcher) {
        val viewModel = form()
        viewModel.form().name.setTextAndPlaceCursorAtEnd("Plant")

        viewModel.events.test {
            assertTrue(viewModel.save())
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
        val posted = environment.server.requestsTo("/api/objects/tasks", HttpMethod.Post).single()
        val body = Json.parseToJsonElement((posted.body as TextContent).text).jsonObject
        assertEquals("Plant", body.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun saveAndAddAnotherStartsOverWithAnEmptyForm() = runTest(dispatcher) {
        val viewModel = form()
        viewModel.form().name.setTextAndPlaceCursorAtEnd("Plant")

        viewModel.events.test {
            viewModel.saveAndAddAnother { viewModel.save() }
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Created, awaitItem())
        }
        assertTrue(viewModel.isNew)
        assertEquals(1, viewModel.generation)
        assertEquals("", viewModel.form().name.text.toString())
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun aRefusedAddAnotherLeavesTheNextSavePlain() = runTest(dispatcher) {
        val viewModel = form()

        viewModel.saveAndAddAnother { viewModel.save() }
        advanceUntilIdle()
        viewModel.form().name.setTextAndPlaceCursorAtEnd("Plant")

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
        assertEquals(0, viewModel.generation)
    }

    @Test
    fun anOfflineAddAnotherLeavesTheNextSavePlain() = runTest(dispatcher) {
        val viewModel = form()
        viewModel.form().name.setTextAndPlaceCursorAtEnd("Plant")
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.saveAndAddAnother { viewModel.save() }
            advanceUntilIdle()
            assertEquals(ObjectFormEvent.Offline, awaitItem())

            environment.server.unreachable = false
            viewModel.save()
            advanceUntilIdle()
            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
    }

    @Test
    fun aDisabledCategoryStaysAChoiceOnlyForTheTaskThatHasIt() {
        val all = listOf(
            TaskCategory(id = 1, name = "Garden"),
            TaskCategory(id = 2, name = "Old", active = false),
        )

        assertEquals(listOf(1), taskCategoryChoices(all, null).map { it.id })
        assertEquals(listOf(1, 2), taskCategoryChoices(all, 2).map { it.id })
    }
}
