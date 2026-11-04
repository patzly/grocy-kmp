package com.patrickzedler.grocy.feature.masterdata.impl.userobjects

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.forms.ObjectFormEvent
import com.patrickzedler.grocy.core.forms.ObjectFormLoad
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.Userobject
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class UserobjectsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server

    private val title = field("title", "Title", column = true, sort = 1)
    private val pages = field("pages", "Pages", UserfieldType.NUMBER_INTEGRAL, column = true, 2)
    private val read = field("read", "Read", UserfieldType.CHECKBOX, column = true, sort = 3)
    private val note = field("note", "Note", column = false, sort = 4)

    private fun field(
        name: String,
        caption: String,
        type: UserfieldType = UserfieldType.TEXT_SINGLE_LINE,
        column: Boolean,
        sort: Int,
    ) = Userfield(
        id = sort,
        entity = "userentity-books",
        name = name,
        caption = caption,
        type = type,
        showAsColumnInTables = column,
        sortNumber = sort,
    )

    private val fields get() = listOf(title, pages, read, note)

    private fun book(id: Int, vararg values: Pair<String, String?>) =
        Userobject(id, userentityId = 1, userfields = mapOf(*values))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server.respond(
            "/api/objects/userfields",
            body = """[{"id":1,"entity":"userentity-books","name":"title","caption":"Title",""" +
                """"type":"text-single-line","show_as_column_in_tables":1}]""",
        )
        server.respond(
            "/api/objects/userobjects",
            body = """[{"id":1,"userentity_id":1},{"id":2,"userentity_id":1},""" +
                """{"id":3,"userentity_id":2}]""",
        )
        server.respond("/api/userfields/userentity-books/1", body = """{"title":"Dune"}""")
        server.respond("/api/userfields/userentity-books/2", body = """{"title":"Atlas"}""")
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.listViewModel(): UserobjectsViewModel {
        environment.repository.refresh()
        return UserobjectsViewModel("books", environment.repository).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun objectsOfTheEntityComeWithTheirValuesSortedByTheFirstColumn() = runTest(dispatcher) {
        val viewModel = listViewModel()

        val state = requireNotNull(viewModel.state.value)
        assertEquals("Books", state.userentity?.caption)
        assertEquals(listOf("Atlas", "Dune"), state.rows.map { it.title })
    }

    @Test
    fun onlyTheEntitysObjectsAreAskedForTheirValues() = runTest(dispatcher) {
        listViewModel()

        val paths = server.requestedPaths.filter { it.startsWith("/api/userfields/") }
        assertEquals(
            setOf("/api/userfields/userentity-books/1", "/api/userfields/userentity-books/2"),
            paths.toSet(),
        )
    }

    @Test
    fun anEntityWithoutFieldsAnswersWithAnEmptyList() = runTest(dispatcher) {
        // PHP's empty array is no JSON object
        server.respond("/api/userfields/userentity-books/1", body = "[]")

        val viewModel = listViewModel()

        val rows = requireNotNull(viewModel.state.value).rows
        assertEquals(listOf("Atlas", null), rows.map { it.title })
    }

    @Test
    fun aRefreshRemovesWhatTheServerNoLongerHas() = runTest(dispatcher) {
        val viewModel = listViewModel()
        server.respond("/api/objects/userobjects", body = """[{"id":2,"userentity_id":1}]""")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(2), requireNotNull(viewModel.state.value).rows.map { it.id })
    }

    @Test
    fun theSearchFindsShownValues() = runTest(dispatcher) {
        val viewModel = listViewModel()

        viewModel.searchState.setTextAndPlaceCursorAtEnd("dun")
        Snapshot.sendApplyNotifications()
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals(listOf("Dune"), requireNotNull(viewModel.state.value).rows.map { it.title })
    }

    @Test
    fun rowsShowColumnsOnlyTheFirstNamesTheObject() {
        val rows = buildUserobjectRows(
            objects = listOf(
                book(1, "title" to "Dune", "pages" to "412", "read" to "1", "note" to "Hidden"),
            ),
            fields = fields,
            query = "",
            sort = UserobjectSort(title),
        )

        assertEquals(
            listOf(UserobjectRow(1, "Dune", listOf("Pages: 412", "Read"))),
            rows,
        )
    }

    @Test
    fun anEntityWithoutColumnsShowsItsFieldsInstead() {
        assertEquals(listOf(note), shownUserfields(listOf(note)))
    }

    @Test
    fun picturesAndFilesAreNoText() {
        val picture = field("cover", "Cover", UserfieldType.IMAGE, column = true, sort = 5)

        assertEquals(listOf(title), shownUserfields(listOf(picture, title)))
    }

    @Test
    fun numbersSortByTheirValueAndEmptyValuesComeLast() {
        val objects = listOf(
            book(1, "pages" to "100"),
            book(2, "pages" to "9"),
            book(3),
        )

        val ascending = buildUserobjectRows(objects, fields, "", UserobjectSort(pages))
        val descending = buildUserobjectRows(objects, fields, "", UserobjectSort(pages, false))

        assertEquals(listOf(2, 1, 3), ascending.map { it.id })
        assertEquals(listOf(1, 2, 3), descending.map { it.id })
    }

    @Test
    fun aNewObjectIsCreatedWithItsEntityAndItsValuesAndCloses() = runTest(dispatcher) {
        environment.repository.refresh()
        server.respond(
            "/api/objects/userobjects",
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        server.respond(
            "/api/userfields/userentity-books/${MasterDataJson.CREATED_ID}",
            body = """{"title":"New"}""",
        )
        val viewModel = UserobjectViewModel("books", null, environment.repository)
        advanceUntilIdle()
        val load = assertIs<ObjectFormLoad.Ready<UserobjectFormState>>(viewModel.load)
        load.userfields.single().text.setTextAndPlaceCursorAtEnd("New")

        viewModel.events.test {
            assertTrue(viewModel.save())
            advanceUntilIdle()

            assertEquals(ObjectFormEvent.Saved, awaitItem())
        }

        val created = server.requestsTo("/api/objects/userobjects", HttpMethod.Post).single()
        assertEquals("""{"userentity_id":1}""", (created.body as TextContent).text)
        val values = server.requestsTo(
            "/api/userfields/userentity-books/${MasterDataJson.CREATED_ID}",
            HttpMethod.Put,
        ).single()
        assertEquals("""{"title":"New"}""", (values.body as TextContent).text)
    }

    @Test
    fun anExistingObjectOpensWithItsValues() = runTest(dispatcher) {
        listViewModel()

        val viewModel = UserobjectViewModel("books", 1, environment.repository)
        advanceUntilIdle()

        val load = assertIs<ObjectFormLoad.Ready<UserobjectFormState>>(viewModel.load)
        assertEquals("Dune", load.userfields.single().value)
        assertEquals("Books", viewModel.userentity?.caption)
    }

    @Test
    fun anObjectThatIsGoneSaysSo() = runTest(dispatcher) {
        listViewModel()

        val viewModel = UserobjectViewModel("books", 99, environment.repository)
        advanceUntilIdle()

        assertEquals(ObjectFormLoad.NotFound, viewModel.load)
    }

    @Test
    fun deletingRemovesTheStoredCopy() = runTest(dispatcher) {
        val viewModel = listViewModel()
        server.respond(
            "/api/objects/userobjects/1",
            status = HttpStatusCode.NoContent,
            method = HttpMethod.Delete,
        )

        environment.repository.deleteObject(MasterDataRepository.USEROBJECTS, 1)
        advanceUntilIdle()

        assertNull(requireNotNull(viewModel.state.value).rows.find { it.id == 1 })
    }
}
