package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.data.settings.EquipmentOptionsRepository
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormEvent
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.model.equipment.EquipmentField
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class EquipmentViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val settingsStore = FakeSettingsStore()

    private fun manualPath(name: String) =
        "/api/files/equipmentmanuals/${Base64.encode(name.encodeToByteArray())}"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.listViewModel(): EquipmentViewModel {
        val viewModel = EquipmentViewModel(
            environment.repository,
            environment.equipmentRepository,
            EquipmentOptionsRepository(
                settingsStore,
                backgroundScope + UnconfinedTestDispatcher(testScheduler),
            ),
        )
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private fun detailViewModel(id: Int = 1) = EquipmentDetailViewModel(
        id,
        environment.repository,
        environment.equipmentRepository,
    )

    private fun formViewModel(id: Int? = null) = EquipmentFormViewModel(
        id,
        environment.repository,
        environment.equipmentRepository,
    )

    @Test
    fun theListDownloadsAndCounts() = runTest(dispatcher) {
        val viewModel = listViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(2, state.count)
        assertEquals(
            listOf("Drill", "Ladder"),
            state.groups.single().rows.map { it.equipment.name },
        )
    }

    @Test
    fun tableOptionsAreKept() = runTest(dispatcher) {
        val viewModel = listViewModel()
        advanceUntilIdle()

        viewModel.setSort(TableColumn.OfUserfield("room"), ascending = false)
        viewModel.setGrouping(TableColumn.OfUserfield("room"))
        viewModel.setFieldShown(EquipmentField.USERFIELDS, shown = false)
        advanceUntilIdle()

        val options = settingsStore.equipmentOptions.value
        assertEquals("room", options.sortUserfield)
        assertEquals("room", options.groupingUserfield)
        assertTrue(options.fields.isEmpty())
    }

    @Test
    fun aPieceOpensWithItsManualAndKeepsItWhileSwitchingTabs() = runTest(dispatcher) {
        environment.server.respond(manualPath("abcdrill.pdf"), body = "%PDF-1.7")
        val viewModel = detailViewModel()
        advanceUntilIdle()
        val state = viewModel.state.value!!

        assertEquals("Keep dry", state.notes)
        val tab = viewModel.tabOf(state)
        assertEquals(EquipmentTab.Manual, tab)
        viewModel.load(tab, state.equipment)
        advanceUntilIdle()

        val loaded = assertIs<FileState.Loaded>(viewModel.fileState)
        assertEquals("abcdrill.pdf", loaded.fileName)
        assertTrue(loaded.isPdf)

        viewModel.chooseTab(EquipmentTab.Notes)
        viewModel.load(EquipmentTab.Notes, state.equipment)
        assertEquals(FileState.None, viewModel.fileState)
        viewModel.chooseTab(EquipmentTab.Manual)
        viewModel.load(EquipmentTab.Manual, state.equipment)
        advanceUntilIdle()

        assertIs<FileState.Loaded>(viewModel.fileState)
        assertEquals(1, environment.server.requestsTo(manualPath("abcdrill.pdf"), HttpMethod.Get).size)
    }

    @Test
    fun aPieceWithoutManualOpensWithItsNotes() = runTest(dispatcher) {
        val viewModel = detailViewModel(id = 2)
        advanceUntilIdle()
        val state = viewModel.state.value!!

        assertEquals(EquipmentTab.Notes, viewModel.tabOf(state))
        assertNull(state.notes)
        viewModel.load(EquipmentTab.Manual, state.equipment)
        assertEquals(FileState.None, viewModel.fileState)
    }

    @Test
    fun whatIsNoPdfIsNotShownAsOne() = runTest(dispatcher) {
        environment.server.respond(manualPath("abcdrill.pdf"), body = "<html><script>")
        val viewModel = detailViewModel()
        advanceUntilIdle()
        val state = viewModel.state.value!!

        viewModel.load(EquipmentTab.Manual, state.equipment)
        advanceUntilIdle()

        assertFalse(assertIs<FileState.Loaded>(viewModel.fileState).isPdf)
    }

    @Test
    fun aFailedDownloadCanBeRetriedAndTellsWhenOffline() = runTest(dispatcher) {
        val viewModel = detailViewModel()
        advanceUntilIdle()
        val state = viewModel.state.value!!
        environment.server.respond(
            manualPath("abcdrill.pdf"),
            status = HttpStatusCode.InternalServerError,
        )

        viewModel.load(EquipmentTab.Manual, state.equipment)
        advanceUntilIdle()
        assertEquals(FileState.Failed(isOffline = false), viewModel.fileState)

        environment.server.unreachable = true
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(FileState.Failed(isOffline = true), viewModel.fileState)

        environment.server.unreachable = false
        environment.server.respond(manualPath("abcdrill.pdf"), body = "%PDF-1.7")
        viewModel.retry()
        advanceUntilIdle()
        assertIs<FileState.Loaded>(viewModel.fileState)
    }

    @Test
    fun aFileUserfieldLoadsWithTheNameTheUserKnows() = runTest(dispatcher) {
        environment.server.respond(
            "/api/files/userfiles/eDEucGRm_V2FycmFudHkucGRm",
            body = "%PDF-1.7",
        )
        val viewModel = detailViewModel()
        advanceUntilIdle()
        val state = viewModel.state.value!!

        viewModel.load(EquipmentTab.File("warranty", "Warranty"), state.equipment)
        advanceUntilIdle()

        assertEquals("Warranty.pdf", assertIs<FileState.Loaded>(viewModel.fileState).fileName)
    }

    @Test
    fun aNewPieceIsCreatedAndClosesTheForm() = runTest(dispatcher) {
        val viewModel = formViewModel()
        advanceUntilIdle()
        val form = assertIs<ObjectFormLoad.Ready<EquipmentFormState>>(viewModel.load).form
        form.name.setTextAndPlaceCursorAtEnd("Created equipment")

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()
            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }
        assertEquals(1, environment.server.requestsTo("/api/objects/equipment", HttpMethod.Post).size)
    }

    @Test
    fun withoutANameNothingIsSent() = runTest(dispatcher) {
        val viewModel = formViewModel()
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(FieldError.Required, viewModel.errors[ObjectField.NAME])
        assertTrue(environment.server.requestsTo("/api/objects/equipment", HttpMethod.Post).isEmpty())
    }

    @Test
    fun deletingTheManualHappensOnSave() = runTest(dispatcher) {
        environment.equipmentRepository.refresh()
        val viewModel = formViewModel(id = 1)
        advanceUntilIdle()
        val form = assertIs<ObjectFormLoad.Ready<EquipmentFormState>>(viewModel.load).form
        assertEquals("abcdrill.pdf", viewModel.savedManual)

        form.manual = PictureChange.Delete

        assertTrue(environment.server.requestsTo(manualPath("abcdrill.pdf"), HttpMethod.Delete).isEmpty())
        assertTrue(viewModel.hasUnsavedChanges)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, environment.server.requestsTo(manualPath("abcdrill.pdf"), HttpMethod.Delete).size)
    }

    @Test
    fun aDeletedPieceTellsTheScreen() = runTest(dispatcher) {
        environment.equipmentRepository.refresh()

        environment.repository.deleteObject("equipment", 2)

        assertEquals(
            listOf("Drill"),
            environment.equipmentRepository.equipment.first().map { it.name },
        )
    }
}
