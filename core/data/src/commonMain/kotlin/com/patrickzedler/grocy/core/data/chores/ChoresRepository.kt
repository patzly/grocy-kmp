package com.patrickzedler.grocy.core.data.chores

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.ServerValueEntity
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.chores.ChoreDetails
import com.patrickzedler.grocy.core.model.chores.ChoreExecution
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grocy's chores, offline first: its overview, journal and overview cards as last downloaded.
 * Tracking, undoing and rescheduling are online only and download what the server computed from
 * them, never computing the next execution itself.
 *
 * @param connection the logged in server, null after a logout.
 */
class ChoresRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
) {

    private val dao = database.choreDao()
    private val masterDataDao = database.masterDataDao()

    /** The active chores with when they are due, by name. */
    val current: Flow<List<CurrentChore>> =
        dao.observeCurrent().map { list -> list.map { it.toModel() } }

    /** The logged in user, who Grocy's "assigned to me" and "Done by" mean. */
    val currentUserId: Flow<Int?> =
        masterDataDao.observeServerValue(CURRENT_USER).map { it?.json?.toIntOrNull() }

    /** The journal since [since] ("YYYY-MM-DD", everything without), newest first. */
    fun executions(since: String?): Flow<List<ChoreExecution>> =
        dao.observeExecutions(since.orEmpty()).map { list -> list.map { it.toModel() } }

    /** Chore [choreId]'s overview card, null until it was downloaded once. */
    fun details(choreId: Int): Flow<ChoreDetails?> =
        dao.observeDetails(choreId).map { it?.toModel() }

    /**
     * Downloads the overview and the logged in user.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refresh() {
        val connection = requireConnection()
        val (chores, user) = coroutineScope {
            val chores = async { api.currentChores(connection) }
            val user = async { api.currentUser(connection) }
            chores.await() to user.await()
        }
        database.withWriteTransaction {
            dao.deleteCurrent()
            dao.insertCurrent(chores.map { it.toEntity() })
            masterDataDao.upsertServerValue(ServerValueEntity(CURRENT_USER, user.id.toString()))
        }
    }

    /** Downloads the journal since [since] ("YYYY-MM-DD", everything without). */
    suspend fun refreshExecutions(since: String?) {
        val executions = api.choreExecutions(requireConnection(), since)
        database.withWriteTransaction {
            dao.deleteExecutions(since.orEmpty())
            dao.insertExecutions(executions.map { it.toEntity() })
        }
    }

    /** Downloads chore [choreId]'s overview card. */
    suspend fun refreshDetails(choreId: Int) {
        val details = api.choreDetails(requireConnection(), choreId)
        dao.upsertDetails(details.toEntity())
    }

    /**
     * Tracks an execution of chore [choreId] (Grocy's tracked time) with the changed values of
     * its [userfields]; returns the execution's id, which [undo] takes.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun track(
        choreId: Int,
        trackedTime: String,
        doneByUserId: Int? = null,
        skipped: Boolean = false,
        userfields: Map<String, String?> = emptyMap(),
    ): Int {
        val connection = requireConnection()
        val execution = api.trackChore(connection, choreId, trackedTime, doneByUserId, skipped)
        dao.upsertExecution(execution.toEntity())
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, CHORES_LOG, execution.id, userfields)
        }
        afterChange(choreId)
        return execution.id
    }

    /** Grocy's "Undo" of execution [executionId] of chore [choreId]. */
    suspend fun undo(executionId: Int, choreId: Int) {
        val connection = requireConnection()
        api.undoChoreExecution(connection, executionId)
        afterChange(choreId) {
            dao.upsertExecution(api.choreExecution(connection, executionId).toEntity())
        }
    }

    /** Grocy's "Reschedule next execution"; both null is its "Reset". */
    suspend fun reschedule(choreId: Int, date: String?, assignedUserId: Int?) {
        api.rescheduleChore(requireConnection(), choreId, date, assignedUserId)
        afterChange(choreId)
    }

    /** Prints chore [choreId]'s grocycode on the server's label printer. */
    suspend fun printLabel(choreId: Int) {
        api.printChoreLabel(requireConnection(), choreId)
    }

    /**
     * What the server computed from a change; a failed download never fails the change, which
     * would invite doing it twice. The next refresh gets it.
     */
    private suspend fun afterChange(choreId: Int, also: suspend () -> Unit = {}) {
        try {
            also()
            refresh()
            refreshDetails(choreId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next refresh gets it
        }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    private companion object {
        const val CHORES_LOG = "chores_log"
        const val CURRENT_USER = "current_user_id"
    }
}
