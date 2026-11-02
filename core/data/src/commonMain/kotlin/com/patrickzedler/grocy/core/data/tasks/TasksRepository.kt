package com.patrickzedler.grocy.core.data.tasks

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.ServerValueEntity
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grocy's tasks, offline first: as last downloaded, also the done ones. Saving, completing and
 * undoing are online only and store what the server answers, deleting goes through
 * [com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository.deleteObject].
 *
 * @param connection the logged in server, null after a logout.
 */
class TasksRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
) {

    private val dao = database.taskDao()
    private val masterDataDao = database.masterDataDao()

    /** Every task, by name. */
    val tasks: Flow<List<Task>> = dao.observeTasks().map { list -> list.map { it.toModel() } }

    /** The logged in user, who Grocy's form assigns a new task to. */
    val currentUserId: Flow<Int?> =
        masterDataDao.observeServerValue(CURRENT_USER).map { it?.json?.toIntOrNull() }

    /**
     * Downloads the tasks, their categories and the logged in user.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refresh() {
        val connection = requireConnection()
        val (tasks, categories, user) = coroutineScope {
            val tasks = async { api.tasks(connection) }
            val categories = async { api.taskCategories(connection) }
            val user = async { api.currentUser(connection) }
            Triple(tasks.await(), categories.await(), user.await())
        }
        database.withWriteTransaction {
            dao.deleteTasks()
            dao.insertTasks(tasks.map { it.toEntity() })
            masterDataDao.deleteTaskCategories()
            masterDataDao.insertTaskCategories(categories.map { it.toEntity() })
            masterDataDao.upsertServerValue(ServerValueEntity(CURRENT_USER, user.id.toString()))
        }
    }

    /**
     * Saves [task], a new one for the id of a new object, with the changed values of its
     * [userfields]; returns its id.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun save(task: Task, userfields: Map<String, String?> = emptyMap()): Int {
        val connection = requireConnection()
        val id = api.saveTask(connection, task)
        if (userfields.isNotEmpty()) api.setUserfields(connection, TASKS, id, userfields)
        dao.upsertTask(api.task(connection, id).toEntity())
        return id
    }

    /** Grocy's "Mark task as completed" at [doneTime] ("YYYY-MM-DD HH:MM:SS"). */
    suspend fun complete(taskId: Int, doneTime: String) {
        val connection = requireConnection()
        api.completeTask(connection, taskId, doneTime)
        download(connection, taskId)
    }

    /** Grocy's "Undo task": it is to do again. */
    suspend fun undo(taskId: Int) {
        val connection = requireConnection()
        api.undoTask(connection, taskId)
        download(connection, taskId)
    }

    /**
     * The task as the server stored a change; a failed download never fails the change, which
     * would invite doing it twice. The next refresh gets it.
     */
    private suspend fun download(connection: ServerConnection, taskId: Int) {
        try {
            dao.upsertTask(api.task(connection, taskId).toEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next refresh gets it
        }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    private companion object {
        const val TASKS = "tasks"
        const val CURRENT_USER = "current_user_id"
    }
}
