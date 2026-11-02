package com.patrickzedler.grocy.core.data.tasks

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TasksRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val repository = TasksRepository(
        server.api,
        database,
        { ServerConnection.ApiKey("https://grocy.test", "k") },
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun bodyOf(path: String, method: HttpMethod = HttpMethod.Post) =
        server.requestsTo(path, method).map {
            Json.parseToJsonElement((it.body as TextContent).text).jsonObject
        }

    @Test
    fun refreshStoresTasksWithTheDoneOnesCategoriesAndTheUser() = runTest {
        repository.refresh()

        val tasks = repository.tasks.first()
        assertEquals(listOf("Buy seeds", "Mow the lawn", "Old task"), tasks.map { it.name })
        val mow = tasks[1]
        assertEquals("2026-11-01", mow.dueDate)
        assertEquals(1, mow.categoryId)
        assertEquals(1, mow.assignedToUserId)
        assertEquals("Mind the roses", mow.description)
        assertEquals(mapOf("area" to "Back"), mow.userfields)
        assertFalse(mow.done)
        assertNull(tasks[0].dueDate)
        assertNull(tasks[0].categoryId)
        assertTrue(tasks[2].done)
        assertEquals("2026-10-01 10:00:00", tasks[2].doneTimestamp)
        assertEquals(1, repository.currentUserId.first())
        assertEquals(1, database.masterDataDao().observeTaskCategories().first().size)
    }

    @Test
    fun refreshReplacesTasksDeletedOnTheServer() = runTest {
        repository.refresh()
        server.respond("/api/objects/tasks", body = "[]")

        repository.refresh()

        assertTrue(repository.tasks.first().isEmpty())
    }

    @Test
    fun aNewTaskIsPostedAndStoredAsTheServerAnswers() = runTest {
        val task = Task(
            id = MasterDataRepository.NEW_OBJECT_ID,
            name = "Plant",
            dueDate = "2026-11-05",
            categoryId = 1,
            assignedToUserId = 2,
        )

        val id = repository.save(task)

        assertEquals(MasterDataJson.CREATED_ID, id)
        val body = bodyOf("/api/objects/tasks").single()
        assertEquals("Plant", body.getValue("name").jsonPrimitive.content)
        assertEquals("2026-11-05", body.getValue("due_date").jsonPrimitive.content)
        assertEquals(1, body.getValue("category_id").jsonPrimitive.int)
        assertEquals(2, body.getValue("assigned_to_user_id").jsonPrimitive.int)
        assertEquals("Created task", repository.tasks.first().single().name)
    }

    @Test
    fun savingNeverSendsWhetherTheTaskIsDone() = runTest {
        repository.save(Task(id = 1, name = "Mow the lawn", done = true))

        val body = bodyOf("/api/objects/tasks/1", HttpMethod.Put).single()
        assertFalse("done" in body)
        assertEquals(JsonNull, body.getValue("due_date"))
        assertEquals(JsonNull, body.getValue("category_id"))
    }

    @Test
    fun changedUserfieldsAreSent() = runTest {
        repository.save(Task(id = 1, name = "Mow the lawn"), mapOf("area" to "Front"))

        val body = bodyOf("/api/userfields/tasks/1", HttpMethod.Put).single()
        assertEquals("Front", body.getValue("area").jsonPrimitive.content)
    }

    @Test
    fun completingSendsTheTimeAndStoresWhatTheServerAnswers() = runTest {
        repository.refresh()
        server.respond("/api/objects/tasks/1", body = MasterDataJson.COMPLETED_TASK)

        repository.complete(1, "2026-11-02 12:00:00")

        val body = bodyOf("/api/tasks/1/complete").single()
        assertEquals("2026-11-02 12:00:00", body.getValue("done_time").jsonPrimitive.content)
        assertTrue(repository.tasks.first().first { it.id == 1 }.done)
    }

    @Test
    fun undoingStoresWhatTheServerAnswers() = runTest {
        repository.refresh()
        server.respond("/api/objects/tasks/3", body = MasterDataJson.UNDONE_TASK)

        repository.undo(3)

        assertEquals(1, server.requestsTo("/api/tasks/3/undo", HttpMethod.Post).size)
        assertFalse(repository.tasks.first().first { it.id == 3 }.done)
    }

    @Test
    fun aFailedDownloadAfterCompletingDoesNotFailIt() = runTest {
        repository.refresh()
        server.respond("/api/objects/tasks/1", status = HttpStatusCode.InternalServerError)

        repository.complete(1, "2026-11-02 12:00:00")

        assertFalse(repository.tasks.first().first { it.id == 1 }.done)
    }

    @Test
    fun aRejectedCompletionFails() = runTest {
        server.respond(
            "/api/tasks/1/complete",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"Task does not exist"}""",
            method = HttpMethod.Post,
        )

        assertFailsWith<GrocyApiException.HttpError> {
            repository.complete(1, "2026-11-02 12:00:00")
        }
    }

    @Test
    fun deletingGoesThroughTheMasterDataRepository() = runTest {
        val masterData = MasterDataRepository(
            server.api,
            database,
            { ServerConnection.ApiKey("https://grocy.test", "k") },
        )
        repository.refresh()

        assertEquals("Mow the lawn", masterData.taskName(1))
        masterData.deleteObject(MasterDataRepository.TASKS, 1)

        assertEquals(1, server.requestsTo("/api/objects/tasks/1", HttpMethod.Delete).size)
        assertTrue(repository.tasks.first().none { it.id == 1 })
    }
}
