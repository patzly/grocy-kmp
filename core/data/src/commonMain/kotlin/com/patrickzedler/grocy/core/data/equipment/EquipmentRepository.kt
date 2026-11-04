package com.patrickzedler.grocy.core.data.equipment

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.data.masterdata.cleanFileName
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.equipment.Equipment
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grocy's equipment, offline first: its list and notes as last downloaded. Saving is online only
 * and stores what the server saved. Instruction manuals and other files are downloaded when they
 * are shown, they can be large.
 *
 * @param connection the logged in server, null after a logout.
 */
class EquipmentRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
    private val random: Random = Random.Default,
) {

    private val dao = database.equipmentDao()

    /** Every piece of equipment, by name. */
    val equipment: Flow<List<Equipment>> =
        dao.observeEquipment().map { list -> list.map { it.toModel() } }

    /**
     * Downloads the equipment.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refresh() {
        val equipment = api.equipment(requireConnection())
        database.withWriteTransaction {
            dao.deleteEquipment()
            dao.insertEquipment(equipment.map { it.toEntity() })
        }
    }

    /**
     * Saves [equipment], a new one for the id of a new object, with the changed values of its
     * [userfields] and what happens to its instruction [manual]; returns its id. A replaced or
     * deleted file goes away last, a failure before leaves the piece as it was.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun save(
        equipment: Equipment,
        userfields: Map<String, String?> = emptyMap(),
        manual: PictureChange = PictureChange.Keep,
    ): Int {
        val connection = requireConnection()
        val old = equipment.instructionManualFileName
        val newFile = (manual as? PictureChange.Replace)?.let { manualFileName(it.fileName) }
        val fileName = when (manual) {
            PictureChange.Keep -> old
            PictureChange.Delete -> null
            is PictureChange.Replace -> newFile
        }
        // The file first: the object must never name a file the server does not have
        if (manual is PictureChange.Replace && newFile != null) {
            api.uploadEquipmentManual(connection, newFile, manual.bytes)
        }
        val id = api.saveEquipment(
            connection,
            equipment.copy(instructionManualFileName = fileName),
        )
        if (userfields.isNotEmpty()) api.setUserfields(connection, EQUIPMENT, id, userfields)
        dao.upsertEquipment(api.equipment(connection, id).toEntity())
        if (manual != PictureChange.Keep && old != null) deleteFile(connection, old)
        return id
    }

    /** Instruction manual [fileName] as stored on the server. */
    suspend fun manual(fileName: String): ByteArray =
        api.equipmentManual(requireConnection(), fileName)

    /** The file of a file userfield with the stored [value]. */
    suspend fun userfieldFile(value: String): ByteArray =
        api.userfieldFile(requireConnection(), value)

    /** A file left behind takes room on the server, which is no reason to fail the save. */
    private suspend fun deleteFile(connection: ServerConnection, fileName: String) {
        try {
            api.deleteEquipmentManual(connection, fileName)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // Stays on the server, nothing refers to it anymore
        }
    }

    private fun manualFileName(original: String): String {
        val prefix = List(RANDOM_NAME_LENGTH) { RANDOM_NAME_CHARS.random(random) }.joinToString("")
        return prefix + cleanFileName(original)
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    private companion object {
        const val EQUIPMENT = "equipment"
        const val RANDOM_NAME_LENGTH = 10
        const val RANDOM_NAME_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
