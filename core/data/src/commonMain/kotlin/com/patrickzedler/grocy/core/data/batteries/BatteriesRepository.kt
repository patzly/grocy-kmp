package com.patrickzedler.grocy.core.data.batteries

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.batteries.BatteryChargeCycle
import com.patrickzedler.grocy.core.model.batteries.BatteryDetails
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grocy's batteries, offline first: its overview, journal and overview cards as last downloaded.
 * Tracking and undoing are online only and download what the server computed from them, never
 * computing the next charge cycle itself.
 *
 * @param connection the logged in server, null after a logout.
 */
class BatteriesRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
) {

    private val dao = database.batteryDao()
    private val masterDataDao = database.masterDataDao()

    /** The active batteries with when they are due. */
    val current: Flow<List<CurrentBattery>> =
        dao.observeCurrent().map { list -> list.map { it.toModel() } }

    /** The journal since [since] ("YYYY-MM-DD", everything without), newest first. */
    fun chargeCycles(since: String?): Flow<List<BatteryChargeCycle>> =
        dao.observeChargeCycles(since.orEmpty()).map { list -> list.map { it.toModel() } }

    /** Battery [batteryId]'s overview card, null until it was downloaded once. */
    fun details(batteryId: Int): Flow<BatteryDetails?> =
        dao.observeDetails(batteryId).map { it?.toModel() }

    /**
     * Downloads the overview and the batteries it names.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refresh() {
        val connection = requireConnection()
        val (current, batteries) = coroutineScope {
            val current = async { api.currentBatteries(connection) }
            val batteries = async { api.batteries(connection) }
            current.await() to batteries.await()
        }
        database.withWriteTransaction {
            dao.deleteCurrent()
            dao.insertCurrent(current.map { it.toEntity() })
            masterDataDao.deleteBatteries()
            masterDataDao.insertBatteries(batteries.map { it.toEntity() })
        }
    }

    /** Downloads the journal since [since] ("YYYY-MM-DD", everything without). */
    suspend fun refreshChargeCycles(since: String?) {
        val cycles = api.batteryChargeCycles(requireConnection(), since)
        database.withWriteTransaction {
            dao.deleteChargeCycles(since.orEmpty())
            dao.insertChargeCycles(cycles.map { it.toEntity() })
        }
    }

    /** Downloads battery [batteryId]'s overview card. */
    suspend fun refreshDetails(batteryId: Int) {
        dao.upsertDetails(api.batteryDetails(requireConnection(), batteryId).toEntity())
    }

    /**
     * Grocy's "Track charge cycle" of battery [batteryId] at [trackedTime] with the changed values
     * of its [userfields]; returns the charge cycle's id, which [undo] takes.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun track(
        batteryId: Int,
        trackedTime: String,
        userfields: Map<String, String?> = emptyMap(),
    ): Int {
        val connection = requireConnection()
        val cycle = api.trackChargeCycle(connection, batteryId, trackedTime)
        dao.upsertChargeCycle(cycle.toEntity())
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, CHARGE_CYCLES, cycle.id, userfields)
        }
        afterChange(batteryId)
        return cycle.id
    }

    /** Grocy's "Undo charge cycle" of charge cycle [cycleId] of battery [batteryId]. */
    suspend fun undo(cycleId: Int, batteryId: Int) {
        val connection = requireConnection()
        api.undoChargeCycle(connection, cycleId)
        afterChange(batteryId) {
            dao.upsertChargeCycle(api.batteryChargeCycle(connection, cycleId).toEntity())
        }
    }

    /** Prints battery [batteryId]'s grocycode on the server's label printer. */
    suspend fun printLabel(batteryId: Int) {
        api.printBatteryLabel(requireConnection(), batteryId)
    }

    /**
     * What the server computed from a change; a failed download never fails the change, which
     * would invite doing it twice. The next refresh gets it.
     */
    private suspend fun afterChange(batteryId: Int, also: suspend () -> Unit = {}) {
        try {
            also()
            refresh()
            refreshDetails(batteryId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next refresh gets it
        }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    private companion object {
        const val CHARGE_CYCLES = "battery_charge_cycles"
    }
}
