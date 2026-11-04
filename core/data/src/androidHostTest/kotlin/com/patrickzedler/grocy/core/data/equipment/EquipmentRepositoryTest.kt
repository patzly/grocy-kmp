package com.patrickzedler.grocy.core.data.equipment

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.equipment.Equipment
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class EquipmentRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val repository = EquipmentRepository(
        server.api,
        database,
        { ServerConnection.ApiKey("https://grocy.test", "k") },
    )
    private val drill = Equipment(
        id = 1,
        name = "Drill",
        description = "<p>Keep</p>",
        instructionManualFileName = "abcdrill.pdf",
    )

    private fun manualPath(name: String) =
        "/api/files/equipmentmanuals/${Base64.encode(name.encodeToByteArray())}"

    private fun savedBody(path: String) = Json.parseToJsonElement(
        (server.requestsTo(path, HttpMethod.Put).single().body as TextContent).text,
    ).jsonObject

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresTheEquipmentByName() = runTest {
        repository.refresh()

        val equipment = repository.equipment.first()
        assertEquals(listOf("Drill", "Ladder"), equipment.map { it.name })
        assertEquals("abcdrill.pdf", equipment[0].instructionManualFileName)
        assertEquals("Garage", equipment[0].userfields["room"])
        assertNull(equipment[1].instructionManualFileName)
        assertNull(equipment[1].description)
    }

    @Test
    fun aNewPieceIsCreated() = runTest {
        val id = repository.save(
            Equipment(MasterDataRepository.NEW_OBJECT_ID, "Created equipment"),
        )

        assertEquals(MasterDataJson.CREATED_ID, id)
        assertEquals(1, server.requestsTo("/api/objects/equipment", HttpMethod.Post).size)
        assertEquals("Created equipment", repository.equipment.first().single().name)
    }

    @Test
    fun keepingTheManualSendsItsNameAndTouchesNoFile() = runTest {
        repository.save(drill)

        assertEquals(
            "abcdrill.pdf",
            savedBody("/api/objects/equipment/1")
                .getValue("instruction_manual_file_name").jsonPrimitive.content,
        )
        assertTrue(server.requestsTo(manualPath("abcdrill.pdf"), HttpMethod.Delete).isEmpty())
    }

    @Test
    fun deletingTheManualSendsNullAndRemovesTheFile() = runTest {
        repository.save(drill, manual = PictureChange.Delete)

        assertEquals(
            JsonNull,
            savedBody("/api/objects/equipment/1").getValue("instruction_manual_file_name"),
        )
        assertEquals(1, server.requestsTo(manualPath("abcdrill.pdf"), HttpMethod.Delete).size)
    }

    @Test
    fun aNewManualIsUploadedUnderANewNameAndTheOldFileGoes() = runTest {
        val bytes = "%PDF-1".encodeToByteArray()

        repository.save(drill, manual = PictureChange.Replace(bytes, "Mein Handbuch ü.pdf"))

        val name = savedBody("/api/objects/equipment/1")
            .getValue("instruction_manual_file_name").jsonPrimitive.content
        assertTrue(name.endsWith("meinhandbuchue.pdf"), name)
        assertTrue(name != "meinhandbuchue.pdf", "Grocy never overwrites a file")
        assertEquals(1, server.requestsTo(manualPath(name), HttpMethod.Put).size)
        assertEquals(1, server.requestsTo(manualPath("abcdrill.pdf"), HttpMethod.Delete).size)
    }

    @Test
    fun aFailedUploadLeavesThePieceAsItWas() = runTest {
        repository.refresh()
        val seeded = EquipmentRepository(
            server.api,
            database,
            { ServerConnection.ApiKey("https://grocy.test", "k") },
            random = Random(1),
        )
        val prefix = Random(1).let { random ->
            List(10) { "abcdefghijklmnopqrstuvwxyz0123456789".random(random) }.joinToString("")
        }
        server.respond(
            manualPath("${prefix}new.pdf"),
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Put,
        )

        assertFailsWith<GrocyApiException> {
            seeded.save(drill, manual = PictureChange.Replace(ByteArray(1), "new.pdf"))
        }

        assertTrue(server.requestsTo("/api/objects/equipment/1", HttpMethod.Put).isEmpty())
        assertEquals(
            "abcdrill.pdf",
            repository.equipment.first().first().instructionManualFileName,
        )
    }

    @Test
    fun aFileThatStaysBehindDoesNotFailTheSave() = runTest {
        server.respond(
            manualPath("abcdrill.pdf"),
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Delete,
        )

        assertEquals(1, repository.save(drill, manual = PictureChange.Delete))
    }

    @Test
    fun manualsAndUserfieldFilesAreDownloadedByTheirStoredNames() = runTest {
        server.respond(manualPath("abcdrill.pdf"), body = "%PDF-manual")
        server.respond("/api/files/userfiles/eDEucGRm_V2FycmFudHkucGRm", body = "%PDF-file")

        assertContentEquals(
            "%PDF-manual".encodeToByteArray(),
            repository.manual("abcdrill.pdf"),
        )
        assertContentEquals(
            "%PDF-file".encodeToByteArray(),
            repository.userfieldFile("eDEucGRm_V2FycmFudHkucGRm"),
        )
    }

    @Test
    fun aDeletedPieceLeavesTheDatabase() = runTest {
        val objects = MasterDataRepository(
            server.api,
            database,
            { ServerConnection.ApiKey("https://grocy.test", "k") },
        )
        repository.refresh()

        objects.deleteObject(MasterDataRepository.EQUIPMENT, 2)

        assertEquals(listOf("Drill"), repository.equipment.first().map { it.name })
    }
}
